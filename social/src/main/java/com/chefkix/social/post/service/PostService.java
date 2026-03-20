package com.chefkix.social.post.service;

import com.chefkix.culinary.api.ContentModerationProvider;
import com.chefkix.culinary.api.SessionProvider;
import com.chefkix.social.group.repository.GroupMemberRepository;
import com.chefkix.social.post.enums.PostStatus;
import com.chefkix.social.post.enums.PostType;
import org.springframework.context.annotation.Lazy;
import com.chefkix.culinary.api.dto.SessionInfo;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.shared.event.PostDeletedEvent;
import com.chefkix.shared.event.PostLikeEvent;
import com.chefkix.social.api.dto.PostDetail;
import com.chefkix.social.api.dto.PostLinkInfo;
import com.chefkix.social.post.dto.request.PostCreationRequest;
import com.chefkix.social.post.dto.request.PostUpdateRequest;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.shared.event.PostCreatedEvent;
import com.chefkix.social.post.dto.response.PostLikeResponse;
import com.chefkix.social.post.dto.response.PostResponse;
import com.chefkix.social.post.dto.response.PostSaveResponse;
import com.chefkix.social.post.entity.CoChef;
import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.entity.PostLike;
import com.chefkix.social.post.entity.PostSave;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.post.mapper.PostMapper;
import com.chefkix.social.post.repository.PostLikeRepository;
import com.chefkix.social.post.repository.PostRepository;
import com.chefkix.social.post.repository.PostSaveRepository;
import com.chefkix.shared.util.UploadImageFile;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostService {

    PostMapper postMapper;
    PostRepository postRepository;
    PostLikeRepository postLikeRepository;
    PostSaveRepository postSaveRepository;
    GroupMemberRepository groupMemberRepository;

    KafkaTemplate<String, Object> kafkaTemplate;

    // Services & Providers
    UploadImageFile uploadImageFile;
    ProfileProvider profileProvider;
    @Lazy SessionProvider sessionProvider;
    ContentModerationProvider contentModerationProvider;

    @Qualifier("taskExecutor")
    Executor taskExecutor;

    @Value("${app.public-base-url:http://localhost:3000}")
    String publicBaseUrl;

    private static final double GRAVITY = 1.8; // Hệ số dùng cho thuật toán Trending (nếu cần sau này)

    // ========================================================================
    // 1. CREATE POST (ASYNC PARALLEL)
    // ========================================================================

    // ========================================================================
    // 1A. CREATE PERSONAL POST
    // ========================================================================
    public PostResponse createPersonalPost(PostCreationRequest request, String userId) {
        log.info("Bắt đầu tạo PERSONAL post cho user: {}", userId);

        // Personal posts go straight to ACTIVE.
        // We pull the isHidden value directly from the DTO.
        return processAndSavePost(request, userId, PostType.PERSONAL, null, PostStatus.ACTIVE, request.getIsPrivateRecipe());
    }

    // ========================================================================
    // 1B. CREATE GROUP POST
    // ========================================================================
    public PostResponse createGroupPost(String groupId, PostCreationRequest request, String userId) {
        log.info("Bắt đầu tạo GROUP post trong group {} cho user: {}", groupId, userId);

        // 1. SECURITY CHECK: Ensure they are in the group
        boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
        if (!isMember) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION, "You must join the group to post here.");
        }

        // 2. STATUS: Defaulting to ACTIVE (You can change this to PENDING later if groups need admin approval)
        PostStatus initialStatus = PostStatus.ACTIVE;

        // Group posts shouldn't be "hidden" from the group, so we force isHidden = false
        return processAndSavePost(request, userId, PostType.GROUP, groupId, initialStatus, false);
    }

    // ========================================================================
    // 1C. SHARED ASYNC UPLOAD ENGINE
    // ========================================================================
    private PostResponse processAndSavePost(PostCreationRequest request, String userId,
                                            PostType type, String groupId,
                                            PostStatus status, boolean isHidden) {
        long startTime = System.currentTimeMillis();

        log.info("Bắt đầu tạo post cho user: {}", userId);

        try {
            // Task A: Upload ảnh (Chạy song song)
            List<CompletableFuture<String>> uploadFutures = new ArrayList<>();
            if (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
                for (MultipartFile photo : request.getPhotoUrls()) {
                    uploadFutures.add(CompletableFuture.supplyAsync(
                            () -> uploadImageFile.uploadImageFile(photo),
                            taskExecutor
                    ));
                }
            }

            // Task B: Get Profile (with fallback)
            CompletableFuture<BasicProfileInfo> profileFuture = CompletableFuture.supplyAsync(
                    () -> {
                        BasicProfileInfo profile = profileProvider.getBasicProfile(userId);
                        if (profile == null) {
                            throw new AppException(ErrorCode.USER_NOT_FOUND, "Profile not found for post creator");
                        }
                        return profile;
                    },
                    taskExecutor
            );

            // Task C: Get Session (if provided)
            CompletableFuture<SessionInfo> sessionFuture;
            if (StringUtils.hasText(request.getSessionId())) {
                sessionFuture = CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                SessionInfo session = sessionProvider.getSession(request.getSessionId());
                                if (session == null) throw new AppException(ErrorCode.SESSION_NOT_FOUND, "Fake session ID!");

                                // Validate: must be own session
                                if (!session.getUserId().equals(userId)) {
                                    throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
                                }
                                return session;
                            } catch (AppException e) {
                                throw e;
                            } catch (Exception e) {
                                log.error("Error fetching session [{}]", request.getSessionId(), e);
                                throw new AppException(
                                        ErrorCode.INTERNAL_SERVER_ERROR,
                                        "Failed to load cooking session for post creation");
                            }
                        },
                        taskExecutor
                );
            } else {
                sessionFuture = CompletableFuture.completedFuture(null);
            }

            // CHỜ TẤT CẢ HOÀN THÀNH
            CompletableFuture<Void> allUploads = CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0]));
            CompletableFuture.allOf(allUploads, profileFuture, sessionFuture).join();

            // GOM KẾT QUẢ
            List<String> photoUrls = uploadFutures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            BasicProfileInfo userProfile = profileFuture.join();
            SessionInfo session = sessionFuture.join();

            log.info("Xử lý xong I/O trong {}ms", System.currentTimeMillis() - startTime);

            // AI CONTENT MODERATION
            if (request.getContent() != null && !request.getContent().isBlank()) {
                var moderationResult = contentModerationProvider.moderate(request.getContent(), "post");
                if (moderationResult.isBlocked()) {
                    log.warn("Post content blocked by AI moderation for user {}: {}", userId, moderationResult.reason());
                    throw new AppException(ErrorCode.CONTENT_MODERATION_FAILED);
                }
            }

            // LƯU VÀO DB
            return savePostToDb(request, userId, userProfile, photoUrls, session, type, groupId, status, isHidden);

        } catch (CompletionException e) {
            log.error("Lỗi song song processAndSavePost", e);
            Throwable cause = e.getCause();
            if (cause instanceof AppException) throw (AppException) cause;
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Transactional
    protected PostResponse savePostToDb(PostCreationRequest request, String userId,
                                        BasicProfileInfo profile,
                                        List<String> photoUrls,
                                        SessionInfo session,
                                        PostType type,
                                        String groupId,
                                        PostStatus status,
                                        boolean isHidden) {

        Post post = postMapper.toPost(request);
        post.setUserId(userId);

        post.setDisplayName(profile != null ? profile.getDisplayName() : "Chef User");
        post.setAvatarUrl(profile != null ? profile.getAvatarUrl() : null);

        post.setPhotoUrls(photoUrls);
        post.generateSlug();
        post.setLikes(0);
        post.setCommentCount(0);
        post.setCreatedAt(Instant.now());
        post.setUpdatedAt(Instant.now());

        // 🚀 THE NEW FIELDS MAPPED HERE
        post.setPostType(type);
        post.setGroupId(groupId);
        post.setStatus(status);
        post.setHidden(isHidden);

        // LINK TO SESSION
        if (session != null) {
            post.setSessionId(session.getId());
            post.setRecipeId(session.getRecipeId());
            post.setRecipeTitle(session.getRecipeTitle());
            post.setPrivateRecipe(Boolean.TRUE.equals(request.getIsPrivateRecipe()));
            post.setXpEarned(0.0);

            if (session.getRoomCode() != null && !session.getRoomCode().isBlank()) {
                post.setRoomCode(session.getRoomCode());
                try {
                    var coChefProfiles = sessionProvider.getCoChefs(session.getRoomCode(), userId);
                    if (coChefProfiles != null && !coChefProfiles.isEmpty()) {
                        post.setCoChefs(coChefProfiles.stream()
                                .map(p -> CoChef.builder()
                                        .userId(p.getUserId())
                                        .displayName(p.getDisplayName())
                                        .avatarUrl(p.getAvatarUrl())
                                        .build())
                                .collect(Collectors.toList()));
                    }
                } catch (Exception e) {
                    log.warn("Failed to populate co-chefs for room {}: {}", session.getRoomCode(), e.getMessage());
                }
            }
        }

        post = postRepository.save(post);

        kafkaTemplate.send("post-delivery",
                PostCreatedEvent.builder()
                        .userId(userId)
                        .postId(post.getId())
                        .build());

        return postMapper.toPostResponse(post);
    }

    @Transactional
    protected PostResponse savePostToDb(PostCreationRequest request, String userId,
                                        BasicProfileInfo profile,
                                        List<String> photoUrls,
                                        SessionInfo session) {

        Post post = postMapper.toPost(request);
        post.setUserId(userId);

        post.setDisplayName(profile != null ? profile.getDisplayName() : "Chef User");
        post.setAvatarUrl(profile != null ? profile.getAvatarUrl() : null);

        post.setPhotoUrls(photoUrls);
        post.generateSlug();
        post.setLikes(0);
        post.setCommentCount(0);
        post.setCreatedAt(Instant.now());
        post.setUpdatedAt(Instant.now());

        // LINK TO SESSION (if provided)
        // NOTE: XP is NOT calculated here. XP is awarded when FE calls
        // recipe-service's POST /{sessionId}/link-post endpoint.
        // This just stores the session reference for display purposes.
        if (session != null) {
            post.setSessionId(session.getId());
            post.setRecipeId(session.getRecipeId());
            post.setRecipeTitle(session.getRecipeTitle());
            post.setPrivateRecipe(Boolean.TRUE.equals(request.getIsPrivateRecipe()));
            // xpEarned will be updated by recipe-service via Kafka or internal API
            // after FE calls link-post endpoint
            post.setXpEarned(0.0);

            // Co-cooking attribution — populate co-chefs from room participants
            if (session.getRoomCode() != null && !session.getRoomCode().isBlank()) {
                post.setRoomCode(session.getRoomCode());
                try {
                    var coChefProfiles = sessionProvider.getCoChefs(session.getRoomCode(), userId);
                    if (coChefProfiles != null && !coChefProfiles.isEmpty()) {
                        post.setCoChefs(coChefProfiles.stream()
                                .map(p -> CoChef.builder()
                                        .userId(p.getUserId())
                                        .displayName(p.getDisplayName())
                                        .avatarUrl(p.getAvatarUrl())
                                        .build())
                                .collect(Collectors.toList()));
                    }
                } catch (Exception e) {
                    log.warn("Failed to populate co-chefs for room {}: {}", session.getRoomCode(), e.getMessage());
                }
            }
        }

        post = postRepository.save(post);

        // Publish event so identity module can increment totalRecipesPublished
        kafkaTemplate.send("post-delivery",
                PostCreatedEvent.builder()
                        .userId(userId)
                        .postId(post.getId())
                        .build());

        return postMapper.toPostResponse(post);
    }

    // ========================================================================
    // 2. UPDATE & DELETE
    // ========================================================================

    @Transactional
    public PostResponse updatePost(String postId, PostUpdateRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND, "Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION, "You cannot edit this post");
        }

        // Logic chặn sửa sau 1 tiếng (Tùy business, có thể bỏ nếu muốn)
        Instant oneHourAfterCreation = post.getCreatedAt().plusSeconds(3600);
        if (Instant.now().isAfter(oneHourAfterCreation)) {
            throw new AppException(ErrorCode.POST_EDIT_EXPIRED, "You can no longer edit this post");
        }

        if (StringUtils.hasText(request.getContent())) {
            post.setContent(request.getContent());
            post.generateSlug();
            post.setPostUrl(publicBaseUrl + "/post/" + post.getId());
        }

        if (request.getTags() != null) {
            post.setTags(request.getTags());
        }

        post.setUpdatedAt(Instant.now());
        postRepository.save(post);
        return postMapper.toPostResponse(post);
    }

    @Transactional
    public void deletePost(Authentication authentication, String postId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND, "Post not found"));

        if (!post.getUserId().equals(userId)) {
            // Admin có thể xóa (nếu cần check role), ở đây check đơn giản
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        postRepository.delete(post);

        PostDeletedEvent postDeletedEvent = PostDeletedEvent.builder()
                .userId(userId)
                .postId(post.getId())
                .build();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    kafkaTemplate.send("post-deleted-delivery", postDeletedEvent);
                }
            });
        } else {
            kafkaTemplate.send("post-deleted-delivery", postDeletedEvent);
        }
    }

    // ========================================================================
    // 3. LIKE & UNLIKE (IDEMPOTENT)
    // ========================================================================

    /**
     * Toggle like on a post.
     * If not liked: adds like. If already liked: removes like.
     */
    @Transactional
    public PostLikeResponse toggleLike(String postId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean alreadyLiked = postLikeRepository.existsByPostIdAndUserId(postId, userId);
        
        if (alreadyLiked) {
            return unlikePost(postId);
        } else {
            return likePost(postId);
        }
    }

    @Transactional
    public PostLikeResponse likePost(String postId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        // Idempotency Check
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            return PostLikeResponse.builder().isLiked(true).likeCount(post.getLikes()).build();
        }

        PostLike postLike = new PostLike();
        postLike.setUserId(userId);
        postLike.setPostId(postId);
        postLike.setCreatedDate(LocalDateTime.now());
        postLikeRepository.save(postLike);

        post.setLikes(post.getLikes() + 1);
        post.setUpdatedAt(Instant.now()); // Update time để thuật toán trending tính lại
        postRepository.save(post);

        sendLikeNotification(userId, post);

        return PostLikeResponse.builder().isLiked(true).likeCount(post.getLikes()).build();
    }

    @Transactional
    public PostLikeResponse unlikePost(String postId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        var postLike = postLikeRepository.findByPostIdAndUserId(postId, userId);

        if (postLike == null) {
            return PostLikeResponse.builder().isLiked(false).likeCount(post.getLikes()).build();
        }

        postLikeRepository.delete(postLike);

        post.setLikes(Math.max(post.getLikes() - 1, 0));
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);

        return PostLikeResponse.builder().isLiked(false).likeCount(post.getLikes()).build();
    }

    private void sendLikeNotification(String userId, Post post) {
        // Nên chạy Async hoặc Fire-and-forget để không chặn luồng chính
        CompletableFuture.runAsync(() -> {
            try {
                    BasicProfileInfo profile = null;
                try {
                    profile = profileProvider.getBasicProfile(userId);
                } catch (Exception ignored) {}
                String displayName = profile != null ? profile.getDisplayName() : "Someone";
                String avatarUrl = profile != null ? profile.getAvatarUrl() : null;

                PostLikeEvent event = PostLikeEvent.builder()
                        .likerId(userId)
                        .postOwnerId(post.getUserId())
                        .postId(post.getId())
                        .content(getPostContentPreview(post))
                        .displayName(displayName)
                        .likerAvatarUrl(avatarUrl)
                        .build();

                kafkaTemplate.send("post-liked-delivery", event);
            } catch (Exception e) {
                log.error("Error sending like notification", e);
            }
        }, taskExecutor); // Tận dụng lại executor
    }

    // ========================================================================
    // 4. FEED / GET POSTS
    // ========================================================================

    public Page<PostResponse> getAllPosts(int mode, Pageable pageable, String currentUserId) {
        // mode = 1: Trending, mode = 0: Latest
        Sort sort = (mode == 1) ? Sort.by("hotScore").descending() : Sort.by("createdAt").descending();

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );

        Page<PostResponse> page = postRepository.findByHiddenFalse(sortedPageable)
                .map(postMapper::toPostResponse);
        enrichPageWithUserStatus(page, currentUserId);
        return page;
    }

    public Page<PostResponse> getAllPostsByUserId(String userId, Pageable pageable, String currentUserId) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("createdAt").descending()
        );
        Page<PostResponse> page = postRepository.findByUserIdAndHiddenFalseOrderByCreatedAtDesc(userId, sortedPageable)
                .map(postMapper::toPostResponse);
        enrichPageWithUserStatus(page, currentUserId);
        return page;
    }

    /**
     * Personalized "Following" feed — posts from users the current user follows, plus their own.
     * Uses one-directional follow list (not mutual-only), matching Instagram/Twitter behavior.
     *
     * @param mode 0 = latest (createdAt desc), 1 = trending (hotScore desc)
     * @param pageable pagination params
     * @param currentUserId the authenticated user
     * @return paginated feed of posts from followed users + self
     */
    public Page<PostResponse> getFollowingFeed(int mode, Pageable pageable, String currentUserId) {
        // Get one-directional following list (everyone the user follows)
        List<String> followingIds = new ArrayList<>(profileProvider.getFollowingIds(currentUserId));
        // Include user's own posts — Instagram/Twitter convention
        followingIds.add(currentUserId);

        // Defensive: empty $in query would match nothing or error in some drivers
        if (followingIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<Post> posts = (mode == 1)
                ? postRepository.findByUserIdInAndHiddenFalseOrderByHotScoreDesc(followingIds, pageable)
                : postRepository.findByUserIdInAndHiddenFalseOrderByCreatedAtDesc(followingIds, pageable);

        Page<PostResponse> page = posts.map(postMapper::toPostResponse);
        enrichPageWithUserStatus(page, currentUserId);
        return page;
    }

    /**
     * Batch-enriches a page of PostResponses with the current user's like/save status.
     * Uses 2 batch queries instead of 2N individual queries (eliminates N+1).
     */
    private void enrichPageWithUserStatus(Page<PostResponse> page, String currentUserId) {
        List<PostResponse> content = page.getContent();
        if (content.isEmpty() || currentUserId == null || currentUserId.isBlank()) {
            content.forEach(p -> { p.setIsLiked(false); p.setIsSaved(false); });
            return;
        }

        List<String> postIds = content.stream().map(PostResponse::getId).collect(Collectors.toList());

        // 2 batch queries instead of 2*N individual queries
        var likedPosts = postLikeRepository.findByUserIdAndPostIdIn(currentUserId, postIds);
        var savedPosts = postSaveRepository.findByUserIdAndPostIdIn(currentUserId, postIds);

        var likedPostIds = likedPosts.stream()
                .map(like -> like.getPostId())
                .collect(Collectors.toSet());
        var savedPostIds = savedPosts.stream()
                .map(save -> save.getPostId())
                .collect(Collectors.toSet());

        content.forEach(post -> {
            post.setIsLiked(likedPostIds.contains(post.getId()));
            post.setIsSaved(savedPostIds.contains(post.getId()));
        });
    }
    
    /**
     * Enriches a single PostResponse with the current user's like/save status.
     * Used for single-post fetches (detail, update). For feeds, use enrichPageWithUserStatus.
     */
    private PostResponse enrichWithUserStatus(PostResponse post, String currentUserId) {
        if (currentUserId == null || currentUserId.isBlank()) {
            post.setIsLiked(false);
            post.setIsSaved(false);
        } else {
            post.setIsLiked(postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId));
            post.setIsSaved(postSaveRepository.existsByPostIdAndUserId(post.getId(), currentUserId));
        }
        return post;
    }

    // --- UTILS ---

    private String getPostContentPreview(Post post) {
        if (post.getContent() == null || post.getContent().isBlank()) return "";
        final int MAX_LENGTH = 50;
        return post.getContent().length() > MAX_LENGTH
                ? post.getContent().substring(0, MAX_LENGTH) + "..."
                : post.getContent();
    }

    public PostLinkInfo linkingResponse(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        int photoCount = post.getPhotoUrls() != null ? post.getPhotoUrls().size() : 0;
        return PostLinkInfo.builder()
                .postId(postId)
                .photoCount(photoCount)
                .userId(post.getUserId())
                .build();
    }

    /**
     * Get post detail for internal use (chat sharing, provider).
     * No privacy check — caller is responsible.
     */
    public PostDetail getPostDetail(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        return PostDetail.builder()
                .id(post.getId())
                .displayName(post.getDisplayName())
                .content(post.getContent())
                .photoUrls(post.getPhotoUrls())
                .recipeTitle(post.getRecipeTitle())
                .recipeId(post.getRecipeId())
                .sessionId(post.getSessionId())
                .privateRecipe(post.isPrivateRecipe())
                .build();
    }

    /**
     * INTERNAL API: Update xpEarned on a post after culinary module calculates it.
     * Called by culinary module via PostProvider after linkSession() completes.
     */
    @Transactional
    public void updatePostXpEarned(String postId, double xpEarned) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        post.setXpEarned(xpEarned);
        postRepository.save(post);
        log.info("Updated post {} with xpEarned={}", postId, xpEarned);
    }

    // ========================================================================
    // 5. SAVE / BOOKMARK POSTS
    // ========================================================================

    /**
     * Toggle save (bookmark) on a post.
     * If not saved: adds save. If already saved: removes save.
     */
    @Transactional
    public PostSaveResponse toggleSave(String postId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Verify post exists
        if (!postRepository.existsById(postId)) {
            throw new AppException(ErrorCode.POST_NOT_FOUND);
        }
        
        boolean alreadySaved = postSaveRepository.existsByPostIdAndUserId(postId, userId);
        
        if (alreadySaved) {
            return unsavePost(postId, userId);
        } else {
            return savePost(postId, userId);
        }
    }

    private PostSaveResponse savePost(String postId, String userId) {
        PostSave postSave = PostSave.builder()
                .postId(postId)
                .userId(userId)
                .createdDate(LocalDateTime.now())
                .build();
        postSaveRepository.save(postSave);
        
        long saveCount = postSaveRepository.countByPostId(postId);
        return PostSaveResponse.builder()
                .isSaved(true)
                .saveCount(saveCount)
                .build();
    }

    private PostSaveResponse unsavePost(String postId, String userId) {
        postSaveRepository.deleteByPostIdAndUserId(postId, userId);
        
        long saveCount = postSaveRepository.countByPostId(postId);
        return PostSaveResponse.builder()
                .isSaved(false)
                .saveCount(saveCount)
                .build();
    }

    /**
     * Get all posts saved/bookmarked by the current user with pagination.
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getSavedPosts(Pageable pageable) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Get saved post IDs for user
        Page<PostSave> savedPosts = postSaveRepository.findByUserIdOrderByCreatedDateDesc(userId, pageable);
        
        // Extract post IDs
        List<String> postIds = savedPosts.getContent().stream()
                .map(PostSave::getPostId)
                .collect(Collectors.toList());
        
        if (postIds.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // Fetch actual posts (preserving order)
        List<Post> posts = postRepository.findAllById(postIds);
        
        // Create a map for quick lookup
        Map<String, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, p -> p));
        
        // Map to responses in saved order, filtering out any deleted posts
        List<PostResponse> responses = postIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .map(postMapper::toPostResponse)
                .collect(Collectors.toList());

        // Batch-enrich like/save status (2 queries instead of 2*N)
        if (!responses.isEmpty()) {
            var likedPosts = postLikeRepository.findByUserIdAndPostIdIn(userId, postIds);
            var savedPostsSet = postSaveRepository.findByUserIdAndPostIdIn(userId, postIds);
            var likedPostIds = likedPosts.stream().map(l -> l.getPostId()).collect(Collectors.toSet());
            var savedPostIds = savedPostsSet.stream().map(s -> s.getPostId()).collect(Collectors.toSet());
            responses.forEach(r -> {
                r.setIsLiked(likedPostIds.contains(r.getId()));
                r.setIsSaved(savedPostIds.contains(r.getId()));
            });
        }

        return new PageImpl<>(responses, pageable, savedPosts.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PostResponse getPostById(String postId, String currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        if (post.isPrivateRecipe() && !post.getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.POST_ACCESS_DENIED);
        }

        PostResponse response = postMapper.toPostResponse(post);
        return enrichWithUserStatus(response, currentUserId);
    }
}