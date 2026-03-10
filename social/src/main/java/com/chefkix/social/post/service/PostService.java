package com.chefkix.social.post.service;

import com.chefkix.culinary.api.SessionProvider;
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
import org.springframework.util.StringUtils;
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

    KafkaTemplate<String, Object> kafkaTemplate;

    // Services & Providers
    UploadImageFile uploadImageFile;
    ProfileProvider profileProvider;
    SessionProvider sessionProvider;

    @Qualifier("taskExecutor")
    Executor taskExecutor;

    private static final double GRAVITY = 1.8; // Hệ số dùng cho thuật toán Trending (nếu cần sau này)

    // ========================================================================
    // 1. CREATE POST (ASYNC PARALLEL)
    // ========================================================================

    public PostResponse createPost(PostCreationRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
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
                        try {
                            return profileProvider.getBasicProfile(userId);
                        } catch (Exception e) {
                            log.warn("Could not fetch profile for user [{}]. Using fallback. Error: {}", userId, e.getMessage());
                            return BasicProfileInfo.builder()
                                    .userId(userId)
                                    .displayName("Anonymous User")
                                    .avatarUrl(null)
                                    .build();
                        }
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
                                if (session == null) return null;

                                // Validate: must be own session
                                if (!session.getUserId().equals(userId)) {
                                    throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
                                }
                                return session;
                            } catch (AppException e) {
                                throw e;
                            } catch (Exception e) {
                                log.error("Error fetching session [{}]: {}", request.getSessionId(), e.getMessage());
                                return null;
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

            // LƯU VÀO DB
            return savePostToDb(request, userId, userProfile, photoUrls, session);

        } catch (CompletionException e) {
            log.error("Lỗi song song createPost", e);
            Throwable cause = e.getCause();
            if (cause instanceof AppException) throw (AppException) cause;
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
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
            post.setPostUrl("http://localhost:8080/posts/" + post.getSlug()); // Nên đưa domain vào file properties
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

        kafkaTemplate.send("post-deleted-delivery", postDeletedEvent);
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

        return postRepository.findByHiddenFalse(sortedPageable)
                .map(postMapper::toPostResponse)
                .map(post -> enrichWithUserStatus(post, currentUserId));
    }

    public Page<PostResponse> getAllPostsByUserId(String userId, Pageable pageable, String currentUserId) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("createdAt").descending()
        );
        return postRepository.findByUserIdAndHiddenFalseOrderByCreatedAtDesc(userId, sortedPageable)
                .map(postMapper::toPostResponse)
                .map(post -> enrichWithUserStatus(post, currentUserId));
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

        return posts
                .map(postMapper::toPostResponse)
                .map(post -> enrichWithUserStatus(post, currentUserId));
    }
    
    /**
     * Enriches a PostResponse with the current user's like/save status.
     * Returns post unchanged if currentUserId is null (unauthenticated).
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
                .map(post -> {
                    PostResponse response = postMapper.toPostResponse(post);
                    return enrichWithUserStatus(response, userId);
                })
                .collect(Collectors.toList());
        
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