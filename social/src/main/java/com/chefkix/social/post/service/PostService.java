package com.chefkix.social.post.service;

import com.chefkix.culinary.api.ContentModerationProvider;
import com.chefkix.culinary.api.SessionProvider;
import com.chefkix.social.group.repository.GroupMemberRepository;
import com.chefkix.social.post.enums.PostStatus;
import com.chefkix.social.post.enums.PostType;
import lombok.experimental.NonFinal;
import org.springframework.context.annotation.Lazy;
import com.chefkix.culinary.api.dto.SessionInfo;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.shared.event.PostDeletedEvent;
import com.chefkix.shared.event.PostLikeEvent;
import com.chefkix.shared.event.UserMentionEvent;
import com.chefkix.social.api.dto.PostDetail;
import com.chefkix.social.api.dto.PostLinkInfo;
import com.chefkix.social.api.dto.RecentCookRequest;
import com.chefkix.social.post.events.PostIndexEvent;
import com.chefkix.social.post.dto.request.PostCreationRequest;
import com.chefkix.social.post.dto.request.PostUpdateRequest;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.shared.event.PostCreatedEvent;
import com.chefkix.social.post.dto.response.PostLikeResponse;
import com.chefkix.social.post.dto.response.PostResponse;
import com.chefkix.social.post.dto.response.PostSaveResponse;
import com.chefkix.social.post.dto.response.PollVoteResponse;
import com.chefkix.social.post.entity.CoChef;
import com.chefkix.social.post.entity.PollData;
import com.chefkix.social.post.entity.PollVote;
import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.entity.PostLike;
import com.chefkix.social.post.entity.PostSave;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.post.mapper.PostMapper;
import com.chefkix.social.post.repository.PostLikeRepository;
import com.chefkix.social.post.repository.PostRepository;
import com.chefkix.social.post.repository.PostSaveRepository;
import com.chefkix.social.post.repository.PollVoteRepository;
import com.chefkix.social.post.repository.PlateRatingRepository;
import com.chefkix.social.post.repository.CommentRepository;
import com.chefkix.social.post.entity.PlateRating;
import com.chefkix.social.post.dto.request.PlateRateRequest;
import com.chefkix.social.post.dto.response.PlateRateResponse;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
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
    PollVoteRepository pollVoteRepository;
    PlateRatingRepository plateRatingRepository;
    CommentRepository commentRepository;
    MongoTemplate mongoTemplate;
    GroupMemberRepository groupMemberRepository;

    KafkaTemplate<String, Object> kafkaTemplate;
    ApplicationEventPublisher eventPublisher;

    // Services & Providers
    UploadImageFile uploadImageFile;
    ProfileProvider profileProvider;
    @Lazy SessionProvider sessionProvider;
    ContentModerationProvider contentModerationProvider;

    @Qualifier("taskExecutor")
    Executor taskExecutor;

    @NonFinal
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
        PostType type = request.getPostType() != null ? request.getPostType() : PostType.PERSONAL;
        log.info("Bắt đầu tạo {} post cho user: {}", type, userId);

        // Personal/Quick posts go straight to ACTIVE.
        // We pull the isHidden value directly from the DTO.
        return processAndSavePost(request, userId, type, null, PostStatus.ACTIVE, request.getIsPrivateRecipe());
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
        post.setVerified(profile != null && profile.isVerified());

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

        // POLL DATA
        if (type == PostType.POLL && request.getPollQuestion() != null) {
            post.setPollData(PollData.builder()
                    .question(request.getPollQuestion())
                    .optionA(request.getPollOptionA())
                    .optionB(request.getPollOptionB())
                    .votesA(0)
                    .votesB(0)
                    .build());
        }

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

        // Send mention notifications for tagged users
        final Post savedPost = post;
        if (request.getTaggedUserIds() != null && !request.getTaggedUserIds().isEmpty()) {
            String actorDisplayName = profile != null ? profile.getDisplayName() : "Chef User";
            String actorAvatarUrl = profile != null ? profile.getAvatarUrl() : null;
            String contentPreview = request.getContent() != null && request.getContent().length() > 50
                    ? request.getContent().substring(0, 50) + "..."
                    : request.getContent();

            for (String taggedUserId : request.getTaggedUserIds()) {
                if (taggedUserId.equals(userId)) continue; // Skip self-mentions
                try {
                    UserMentionEvent event = UserMentionEvent.builder()
                            .recipientId(taggedUserId)
                            .sourceId(savedPost.getId())
                            .sourceType("POST")
                            .postId(savedPost.getId())
                            .actorId(userId)
                            .actorDisplayName(actorDisplayName)
                            .actorAvatarUrl(actorAvatarUrl)
                            .contentPreview(contentPreview)
                            .build();
                    kafkaTemplate.send("tag-delivery", event);
                } catch (Exception e) {
                    log.error("Failed to send post mention notification to user {}: {}", taggedUserId, e.getMessage());
                }
            }
        }

        // Publish event so identity module can increment totalRecipesPublished
        kafkaTemplate.send("post-delivery",
                PostCreatedEvent.builder()
                        .userId(userId)
                        .postId(post.getId())
                        .build());

        // Real-time Typesense indexing
        eventPublisher.publishEvent(PostIndexEvent.index(post));

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
        Post savedPost = postRepository.save(post);

        // Real-time Typesense re-indexing
        eventPublisher.publishEvent(PostIndexEvent.index(savedPost));

        return postMapper.toPostResponse(savedPost);
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

        // Cascade: delete all related data before post
        postLikeRepository.deleteAllByPostId(postId);
        postSaveRepository.deleteAllByPostId(postId);
        pollVoteRepository.deleteAllByPostId(postId);
        commentRepository.deleteAllByPostId(postId);

        postRepository.delete(post);

        // Real-time Typesense removal
        eventPublisher.publishEvent(PostIndexEvent.remove(postId));

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

        // Atomic increment to prevent race conditions
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(postId)),
                new Update().inc("likes", 1).set("updatedAt", Instant.now()),
                Post.class);

        if (!userId.equals(post.getUserId())) {
            sendLikeNotification(userId, post);
        }

        long likeCount = postLikeRepository.countByPostId(postId);
        return PostLikeResponse.builder().isLiked(true).likeCount((int) likeCount).build();
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

        // Atomic decrement to prevent race conditions
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(postId)),
                new Update().inc("likes", -1).set("updatedAt", Instant.now()),
                Post.class);

        long likeCount = postLikeRepository.countByPostId(postId);
        return PostLikeResponse.builder().isLiked(false).likeCount((int) likeCount).build();
    }

    private void sendLikeNotification(String userId, Post post) {
        // Nên chạy Async hoặc Fire-and-forget để không chặn luồng chính
        CompletableFuture.runAsync(() -> {
            try {
                    BasicProfileInfo profile = null;
                try {
                    profile = profileProvider.getBasicProfile(userId);
                } catch (Exception e) {
                    log.warn("Failed to fetch profile for like notification, userId={}: {}", userId, e.getMessage());
                }
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
        // mode = 2: For You (personalized taste-based feed)
        if (mode == 2 && currentUserId != null && !currentUserId.isBlank()) {
            return getForYouFeed(pageable, currentUserId);
        }

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

    /**
     * Personalized "For You" feed using weighted 5-signal scoring.
     * Algorithm (from Master Plan):
     *   feed_score = (taste_affinity * 0.35) + (recency * 0.25) + (social_proof * 0.20)
     *                + (diversity_bonus * 0.10) + (seasonal_boost * 0.10)
     *
     * 1. Build weighted taste profile from user's recent liked/saved post tags
     * 2. Fetch broad candidate pool (unseen, non-own posts)
     * 3. Score each candidate with 5-signal formula
     * 4. Sort by composite score, apply content diversity, paginate
     * 5. Cold start: fall back to trending entirely
     */
    private Page<PostResponse> getForYouFeed(Pageable pageable, String currentUserId) {
        Map<String, Double> tasteWeights = buildWeightedTasteProfile(currentUserId);

        // Cold start: no interaction history -> trending fallback
        if (tasteWeights.isEmpty()) {
            return getAllPosts(1, pageable, currentUserId);
        }

        Set<String> interactedPostIds = getInteractedPostIds(currentUserId);

        // Fetch a broad candidate pool (3x page size for scoring + diversity)
        int candidatePoolSize = pageable.getPageSize() * 3;
        int skipCount = pageable.getPageNumber() * pageable.getPageSize();

        Criteria baseCriteria = Criteria.where("hidden").is(false)
                .and("userId").ne(currentUserId)
                .and("status").is(PostStatus.ACTIVE.name());

        if (!interactedPostIds.isEmpty()) {
            baseCriteria = baseCriteria.and("_id").nin(interactedPostIds);
        }

        // Get candidates sorted by createdAt desc (recent bias in candidate selection)
        Query candidateQuery = new Query(baseCriteria)
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .limit(candidatePoolSize + skipCount);

        List<Post> candidates = mongoTemplate.find(candidateQuery, Post.class);

        if (candidates.isEmpty()) {
            return getAllPosts(1, pageable, currentUserId);
        }

        // Find max hotScore for normalization
        double maxHotScore = candidates.stream()
                .mapToDouble(p -> p.getHotScore() != null ? p.getHotScore() : 0.0)
                .max().orElse(1.0);
        if (maxHotScore <= 0) maxHotScore = 1.0;

        // Current month seasonal tags
        Set<String> seasonalTags = getSeasonalTags();

        // Score each candidate
        final double normalizer = maxHotScore;
        List<ScoredPost> scoredPosts = new java.util.ArrayList<>();
        Set<String> seenAuthors = new java.util.HashSet<>();

        for (Post post : candidates) {
            double tasteAffinity = computeTasteAffinity(post, tasteWeights);
            double recency = computeRecencyScore(post);
            double socialProof = computeSocialProof(post, normalizer);
            boolean isNewAuthor = !seenAuthors.contains(post.getUserId());
            seenAuthors.add(post.getUserId());
            double diversityBonus = computeDiversityBonus(post, isNewAuthor);
            double seasonalBoost = computeSeasonalBoost(post, seasonalTags);

            double feedScore = (tasteAffinity * 0.35)
                    + (recency * 0.25)
                    + (socialProof * 0.20)
                    + (diversityBonus * 0.10)
                    + (seasonalBoost * 0.10);

            scoredPosts.add(new ScoredPost(post, feedScore));
        }

        // Sort by composite score descending
        scoredPosts.sort((a, b) -> Double.compare(b.score, a.score));

        // Extract posts, apply diversity, then paginate
        List<Post> rankedPosts = scoredPosts.stream()
                .map(sp -> sp.post)
                .collect(Collectors.toList());

        List<Post> diversifiedPosts = applyContentDiversity(rankedPosts, candidatePoolSize);

        // Manual pagination over scored results
        int fromIdx = Math.min(skipCount, diversifiedPosts.size());
        int toIdx = Math.min(skipCount + pageable.getPageSize(), diversifiedPosts.size());
        List<Post> pageSlice = diversifiedPosts.subList(fromIdx, toIdx);

        List<PostResponse> responses = pageSlice.stream()
                .map(postMapper::toPostResponse)
                .collect(Collectors.toList());

        long total = diversifiedPosts.size();
        Page<PostResponse> page = new PageImpl<>(responses, pageable, total);
        enrichPageWithUserStatus(page, currentUserId);
        return page;
    }

    private record ScoredPost(Post post, double score) {}

    /**
     * Taste affinity: sum of tag weights for matching tags, capped at 1.0.
     */
    private double computeTasteAffinity(Post post, Map<String, Double> tasteWeights) {
        if (post.getTags() == null || post.getTags().isEmpty()) return 0.0;
        double score = 0.0;
        for (String tag : post.getTags()) {
            Double w = tasteWeights.get(tag.toLowerCase().trim());
            if (w != null) score += w;
        }
        return Math.min(score, 1.0);
    }

    /**
     * Recency score: exponential decay. Posts from last hour = ~1.0, 1 day = ~0.5, 7 days = ~0.1.
     */
    private double computeRecencyScore(Post post) {
        if (post.getCreatedAt() == null) return 0.0;
        long ageMinutes = java.time.Duration.between(post.getCreatedAt(), Instant.now()).toMinutes();
        if (ageMinutes < 0) ageMinutes = 0;
        // Half-life of ~24 hours (1440 minutes)
        return Math.exp(-0.000481 * ageMinutes); // ln(2)/1440 ≈ 0.000481
    }

    /**
     * Social proof: normalized hotScore (0..1).
     */
    private double computeSocialProof(Post post, double maxHotScore) {
        double hs = post.getHotScore() != null ? post.getHotScore() : 0.0;
        return hs / maxHotScore;
    }

    /**
     * Diversity bonus: rewards underrepresented content types and new authors.
     */
    private double computeDiversityBonus(Post post, boolean isNewAuthor) {
        double bonus = 0.0;
        // QUICK posts get a small boost to ensure feed variety
        if (PostType.QUICK.equals(post.getPostType())) bonus += 0.3;
        // POLL posts get engagement boost
        if (PostType.POLL.equals(post.getPostType())) bonus += 0.4;
        // RECENT_COOK is social proof of real cooking
        if (PostType.RECENT_COOK.equals(post.getPostType())) bonus += 0.2;
        // First appearance of this author in the feed gets a boost
        if (isNewAuthor) bonus += 0.3;
        return Math.min(bonus, 1.0);
    }

    /**
     * Seasonal boost: rewards posts with tags matching current food season.
     */
    private double computeSeasonalBoost(Post post, Set<String> seasonalTags) {
        if (post.getTags() == null || seasonalTags.isEmpty()) return 0.0;
        long matches = post.getTags().stream()
                .filter(t -> seasonalTags.contains(t.toLowerCase().trim()))
                .count();
        if (matches == 0) return 0.0;
        return Math.min(matches * 0.3, 1.0);
    }

    /**
     * Returns seasonal food tags based on current month (Northern Hemisphere).
     */
    private Set<String> getSeasonalTags() {
        int month = java.time.LocalDate.now().getMonthValue();
        Set<String> tags = new java.util.HashSet<>();

        // Spring: March-May
        if (month >= 3 && month <= 5) {
            tags.addAll(Set.of("spring", "salad", "asparagus", "strawberry", "peas", "herbs", "fresh", "light"));
        }
        // Summer: June-August
        else if (month >= 6 && month <= 8) {
            tags.addAll(Set.of("summer", "bbq", "grilling", "watermelon", "berries", "ice cream", "smoothie", "corn"));
        }
        // Autumn: September-November
        else if (month >= 9 && month <= 11) {
            tags.addAll(Set.of("fall", "autumn", "pumpkin", "squash", "apple", "cinnamon", "soup", "stew", "thanksgiving", "harvest"));
        }
        // Winter: December-February
        else {
            tags.addAll(Set.of("winter", "holiday", "christmas", "comfort food", "hot chocolate", "baking", "cookies", "roast"));
        }
        return tags;
    }

    /**
     * Enforces content diversity on a candidate list:
     * - Max 2 posts per author (prevents feed monopolization by popular accounts)
     * - Interleaves QUICK posts for variety (every 5th slot is QUICK if available)
     */
    private List<Post> applyContentDiversity(List<Post> candidates, int pageSize) {
        Map<String, Integer> authorCounts = new java.util.HashMap<>();
        List<Post> quickPosts = new java.util.ArrayList<>();
        List<Post> regularPosts = new java.util.ArrayList<>();

        for (Post p : candidates) {
            int count = authorCounts.getOrDefault(p.getUserId(), 0);
            if (count >= 2) continue;
            authorCounts.put(p.getUserId(), count + 1);

            if (PostType.QUICK.equals(p.getPostType())) {
                quickPosts.add(p);
            } else {
                regularPosts.add(p);
            }
        }

        List<Post> result = new java.util.ArrayList<>();
        int quickIdx = 0;
        int regularIdx = 0;
        int slot = 0;

        while (result.size() < pageSize && (quickIdx < quickPosts.size() || regularIdx < regularPosts.size())) {
            boolean pickQuick = (slot % 5 == 4) && quickIdx < quickPosts.size();
            if (pickQuick) {
                result.add(quickPosts.get(quickIdx++));
            } else if (regularIdx < regularPosts.size()) {
                result.add(regularPosts.get(regularIdx++));
            } else {
                result.add(quickPosts.get(quickIdx++));
            }
            slot++;
        }

        return result;
    }

    /**
     * Builds a weighted taste profile: tag -> normalized weight (0..1).
     * Saves (2x weight) signal stronger intent than likes.
     */
    private Map<String, Double> buildWeightedTasteProfile(String currentUserId) {
        // Get recent liked posts (last 100)
        Query likeQuery = new Query(Criteria.where("userId").is(currentUserId))
                .with(Sort.by(Sort.Direction.DESC, "createdDate"))
                .limit(100);
        likeQuery.fields().include("postId");
        List<PostLike> recentLikes = mongoTemplate.find(likeQuery, PostLike.class);

        // Get recent saved posts (last 50) — saves signal stronger intent (2x weight)
        Query saveQuery = new Query(Criteria.where("userId").is(currentUserId))
                .with(Sort.by(Sort.Direction.DESC, "createdDate"))
                .limit(50);
        saveQuery.fields().include("postId");
        List<PostSave> recentSaves = mongoTemplate.find(saveQuery, PostSave.class);

        Set<String> likedPostIds = new java.util.HashSet<>();
        recentLikes.forEach(l -> likedPostIds.add(l.getPostId()));
        Set<String> savedPostIds = new java.util.HashSet<>();
        recentSaves.forEach(s -> savedPostIds.add(s.getPostId()));

        Set<String> allPostIds = new java.util.HashSet<>(likedPostIds);
        allPostIds.addAll(savedPostIds);

        if (allPostIds.isEmpty()) return Map.of();

        Query tagQuery = new Query(Criteria.where("_id").in(allPostIds));
        tagQuery.fields().include("tags").include("_id");
        List<Post> posts = mongoTemplate.find(tagQuery, Post.class);

        Map<String, Double> tagWeights = new java.util.HashMap<>();
        for (Post p : posts) {
            if (p.getTags() == null) continue;
            double weight = savedPostIds.contains(p.getId()) ? 2.0 : 1.0;
            for (String tag : p.getTags()) {
                tagWeights.merge(tag.toLowerCase().trim(), weight, Double::sum);
            }
        }

        if (tagWeights.isEmpty()) return Map.of();

        // Normalize to 0..1 range
        double maxWeight = tagWeights.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        Map<String, Double> normalized = new java.util.HashMap<>();
        tagWeights.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(30) // Top 30 taste tags
                .forEach(e -> normalized.put(e.getKey(), e.getValue() / maxWeight));

        return normalized;
    }

    private Set<String> getInteractedPostIds(String currentUserId) {
        Query likeQuery = new Query(Criteria.where("userId").is(currentUserId)).limit(500);
        likeQuery.fields().include("postId");
        List<PostLike> likes = mongoTemplate.find(likeQuery, PostLike.class);

        Query saveQuery = new Query(Criteria.where("userId").is(currentUserId)).limit(500);
        saveQuery.fields().include("postId");
        List<PostSave> saves = mongoTemplate.find(saveQuery, PostSave.class);

        Set<String> ids = new java.util.HashSet<>();
        likes.forEach(l -> ids.add(l.getPostId()));
        saves.forEach(s -> ids.add(s.getPostId()));
        return ids;
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
     * Search posts by content, display name, or tags.
     * Uses case-insensitive regex matching (same pattern as RecipeSpecification).
     *
     * @deprecated FE now uses Typesense via SearchController (/api/v1/search).
     *             This MongoDB regex fallback is kept for backward compatibility.
     */
    @Deprecated
    public Page<PostResponse> searchPosts(String query, Pageable pageable, String currentUserId) {
        if (query == null || query.isBlank()) {
            return Page.empty(pageable);
        }

        String regex = ".*" + java.util.regex.Pattern.quote(query.trim()) + ".*";

        Criteria searchCriteria = new Criteria().andOperator(
                Criteria.where("hidden").is(false),
                new Criteria().orOperator(
                        Criteria.where("content").regex(regex, "i"),
                        Criteria.where("displayName").regex(regex, "i"),
                        Criteria.where("tags").regex(regex, "i"),
                        Criteria.where("recipeTitle").regex(regex, "i")
                )
        );

        Query countQuery = new Query(searchCriteria);
        long total = mongoTemplate.count(countQuery, Post.class);

        Query searchQuery = new Query(searchCriteria)
                .with(Sort.by("createdAt").descending())
                .skip((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize());

        List<Post> posts = mongoTemplate.find(searchQuery, Post.class);
        List<PostResponse> responses = posts.stream()
                .map(postMapper::toPostResponse)
                .collect(Collectors.toList());

        Page<PostResponse> page = new PageImpl<>(responses, pageable, total);
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

        // Batch poll vote lookup for poll posts
        List<String> pollPostIds = content.stream()
                .filter(p -> p.getPostType() == PostType.POLL)
                .map(PostResponse::getId)
                .collect(Collectors.toList());
        Map<String, String> pollVoteMap = new HashMap<>();
        if (!pollPostIds.isEmpty()) {
            for (String pid : pollPostIds) {
                pollVoteRepository.findByPostIdAndUserId(pid, currentUserId)
                        .ifPresent(v -> pollVoteMap.put(pid, v.getOption()));
            }
        }

        // Batch plate rating lookup
        var plateRatings = plateRatingRepository.findByPostIdInAndUserId(postIds, currentUserId);
        var plateRatingMap = plateRatings.stream()
                .collect(Collectors.toMap(PlateRating::getPostId, PlateRating::getRating));

        content.forEach(post -> {
            post.setIsLiked(likedPostIds.contains(post.getId()));
            post.setIsSaved(savedPostIds.contains(post.getId()));
            if (post.getPostType() == PostType.POLL) {
                post.setUserVote(pollVoteMap.get(post.getId()));
            }
            post.setUserPlateRating(plateRatingMap.get(post.getId()));
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
            if (post.getPostType() == PostType.POLL) {
                pollVoteRepository.findByPostIdAndUserId(post.getId(), currentUserId)
                        .ifPresent(v -> post.setUserVote(v.getOption()));
            }
            plateRatingRepository.findByPostIdAndUserId(post.getId(), currentUserId)
                    .ifPresent(pr -> post.setUserPlateRating(pr.getRating()));
        }
        return post;
    }

    // ========================================================================
    // POLL VOTING
    // ========================================================================

    public PollVoteResponse votePoll(String postId, String option, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        if (post.getPostType() != PostType.POLL || post.getPollData() == null) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "This post is not a poll");
        }

        var existingVote = pollVoteRepository.findByPostIdAndUserId(postId, userId);
        PollData poll = post.getPollData();

        if (existingVote.isPresent()) {
            PollVote vote = existingVote.get();
            if (vote.getOption().equals(option)) {
                // Same vote — remove it (toggle off)
                pollVoteRepository.delete(vote);
                if ("A".equals(option)) poll.setVotesA(Math.max(0, poll.getVotesA() - 1));
                else poll.setVotesB(Math.max(0, poll.getVotesB() - 1));
                post.setPollData(poll);
                postRepository.save(post);
                return PollVoteResponse.builder()
                        .userVote(null)
                        .votesA(poll.getVotesA())
                        .votesB(poll.getVotesB())
                        .build();
            } else {
                // Switch vote
                String oldOption = vote.getOption();
                vote.setOption(option);
                pollVoteRepository.save(vote);
                if ("A".equals(oldOption)) poll.setVotesA(Math.max(0, poll.getVotesA() - 1));
                else poll.setVotesB(Math.max(0, poll.getVotesB() - 1));
                if ("A".equals(option)) poll.setVotesA(poll.getVotesA() + 1);
                else poll.setVotesB(poll.getVotesB() + 1);
                post.setPollData(poll);
                postRepository.save(post);
                return PollVoteResponse.builder()
                        .userVote(option)
                        .votesA(poll.getVotesA())
                        .votesB(poll.getVotesB())
                        .build();
            }
        }

        // New vote
        pollVoteRepository.save(PollVote.builder()
                .postId(postId)
                .userId(userId)
                .option(option)
                .build());
        if ("A".equals(option)) poll.setVotesA(poll.getVotesA() + 1);
        else poll.setVotesB(poll.getVotesB() + 1);
        post.setPollData(poll);
        postRepository.save(post);

        return PollVoteResponse.builder()
                .userVote(option)
                .votesA(poll.getVotesA())
                .votesB(poll.getVotesB())
                .build();
    }

    public PlateRateResponse ratePlate(String postId, String rating, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        var existing = plateRatingRepository.findByPostIdAndUserId(postId, userId);

        if (existing.isPresent()) {
            PlateRating pr = existing.get();
            if (pr.getRating().equals(rating)) {
                // Same rating — toggle off
                plateRatingRepository.delete(pr);
                if ("FIRE".equals(rating)) post.setFireCount(Math.max(0, post.getFireCount() - 1));
                else post.setCringeCount(Math.max(0, post.getCringeCount() - 1));
                postRepository.save(post);
                return PlateRateResponse.builder()
                        .userRating(null)
                        .fireCount(post.getFireCount())
                        .cringeCount(post.getCringeCount())
                        .build();
            } else {
                // Switch rating
                String old = pr.getRating();
                pr.setRating(rating);
                plateRatingRepository.save(pr);
                if ("FIRE".equals(old)) post.setFireCount(Math.max(0, post.getFireCount() - 1));
                else post.setCringeCount(Math.max(0, post.getCringeCount() - 1));
                if ("FIRE".equals(rating)) post.setFireCount(post.getFireCount() + 1);
                else post.setCringeCount(post.getCringeCount() + 1);
                postRepository.save(post);
                return PlateRateResponse.builder()
                        .userRating(rating)
                        .fireCount(post.getFireCount())
                        .cringeCount(post.getCringeCount())
                        .build();
            }
        }

        // New rating
        plateRatingRepository.save(PlateRating.builder()
                .postId(postId)
                .userId(userId)
                .rating(rating)
                .build());
        if ("FIRE".equals(rating)) post.setFireCount(post.getFireCount() + 1);
        else post.setCringeCount(post.getCringeCount() + 1);
        postRepository.save(post);

        return PlateRateResponse.builder()
                .userRating(rating)
                .fireCount(post.getFireCount())
                .cringeCount(post.getCringeCount())
                .build();
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

    /**
     * Auto-create a lightweight RECENT_COOK post when a cooking session completes.
     * No photos, no moderation — just metadata visible to followers in the feed.
     */
    @Transactional
    public void createRecentCookPost(RecentCookRequest request) {
        Post post = Post.builder()
                .userId(request.getUserId())
                .content(request.getDisplayName() + " cooked " + request.getRecipeTitle()
                        + " \uD83C\uDF73 " + request.getDurationMinutes() + " min")
                .displayName(request.getDisplayName())
                .avatarUrl(request.getAvatarUrl())
                .sessionId(request.getSessionId())
                .recipeId(request.getRecipeId())
                .recipeTitle(request.getRecipeTitle())
                .postType(PostType.RECENT_COOK)
                .status(PostStatus.ACTIVE)
                .likes(0)
                .commentCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        post.generateSlug();

        // Store cover image URL as a single-element photoUrls for card rendering
        if (request.getCoverImageUrl() != null && !request.getCoverImageUrl().isBlank()) {
            post.setPhotoUrls(List.of(request.getCoverImageUrl()));
        }

        postRepository.save(post);
        log.info("Auto-created RECENT_COOK post for user {} (recipe: {})", request.getUserId(), request.getRecipeTitle());
    }
}