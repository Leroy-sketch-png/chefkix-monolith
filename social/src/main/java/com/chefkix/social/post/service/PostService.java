package com.chefkix.social.post.service;

import com.chefkix.culinary.api.ContentModerationProvider;
import com.chefkix.culinary.api.RecipeProvider;
import com.chefkix.culinary.api.SessionProvider;
import com.chefkix.culinary.api.dto.RecipeSummaryInfo;
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
import com.chefkix.shared.event.XpRewardEvent;
import com.chefkix.social.api.dto.PostDetail;
import com.chefkix.social.api.dto.PostLinkInfo;
import com.chefkix.social.api.dto.RecentCookRequest;
import com.chefkix.social.post.events.PostIndexEvent;
import com.chefkix.social.post.dto.request.PostCreationRequest;
import com.chefkix.social.post.dto.request.PostUpdateRequest;
import com.chefkix.shared.event.PostCreatedEvent;
import com.chefkix.social.post.dto.response.PostLikeResponse;
import com.chefkix.social.post.dto.response.PostResponse;
import com.chefkix.social.post.dto.response.PostSaveResponse;
import com.chefkix.social.post.dto.response.PollVoteResponse;
import com.chefkix.social.post.dto.response.RecipeReviewStatsResponse;
import com.chefkix.social.post.dto.response.BattleVoteResponse;
import com.chefkix.social.post.dto.response.TasteProfileResponse;
import com.chefkix.social.post.entity.CoChef;
import com.chefkix.social.post.entity.PollData;
import com.chefkix.social.post.entity.PollVote;
import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.entity.PostLike;
import com.chefkix.social.post.entity.PostSave;
import com.chefkix.social.post.entity.BattleVote;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.post.mapper.PostMapper;
import com.chefkix.social.post.repository.PostLikeRepository;
import com.chefkix.social.post.repository.PostRepository;
import com.chefkix.social.post.repository.PostSaveRepository;
import com.chefkix.social.post.repository.PollVoteRepository;
import com.chefkix.social.post.repository.PlateRatingRepository;
import com.chefkix.social.post.repository.BattleVoteRepository;
import com.chefkix.social.post.repository.CommentRepository;
import com.chefkix.social.post.repository.ReplyRepository;
import com.chefkix.social.post.repository.CommentLikeRepository;
import com.chefkix.social.post.repository.ReplyLikeRepository;
import com.chefkix.social.post.entity.Reply;
import com.chefkix.social.post.entity.Comment;
import com.chefkix.social.post.entity.PlateRating;
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
import org.springframework.dao.DuplicateKeyException;
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
    BattleVoteRepository battleVoteRepository;
    CommentRepository commentRepository;
    ReplyRepository replyRepository;
    CommentLikeRepository commentLikeRepository;
    ReplyLikeRepository replyLikeRepository;
    MongoTemplate mongoTemplate;
    GroupMemberRepository groupMemberRepository;

    KafkaTemplate<String, Object> kafkaTemplate;
    ApplicationEventPublisher eventPublisher;

    // Services & Providers
    UploadImageFile uploadImageFile;
    ProfileProvider profileProvider;
    @Lazy SessionProvider sessionProvider;
    ContentModerationProvider contentModerationProvider;
    RecipeProvider recipeProvider;

    @Qualifier("taskExecutor")
    Executor taskExecutor;

    @NonFinal
    @Value("${app.public-base-url:http://localhost:3000}")
    String publicBaseUrl;

    // ========================================================================
    // 1. CREATE POST (ASYNC PARALLEL)
    // ========================================================================

    // ========================================================================
    // 1A. CREATE PERSONAL POST
    // ========================================================================
    public PostResponse createPersonalPost(PostCreationRequest request, String userId) {
        PostType type = request.getPostType() != null ? request.getPostType() : PostType.PERSONAL;
        log.info("Starting to create {} post for user: {}", type, userId);

        // Personal/Quick posts go straight to ACTIVE.
        // We pull the isHidden value directly from the DTO.
        return processAndSavePost(request, userId, type, null, PostStatus.ACTIVE, request.getIsPrivateRecipe());
    }

    // ========================================================================
    // 1B. CREATE GROUP POST
    // ========================================================================
    public PostResponse createGroupPost(String groupId, PostCreationRequest request, String userId) {
        log.info("Starting to create GROUP post in group {} for user: {}", groupId, userId);

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

        log.info("Starting to create post for user: {}", userId);

        try {
            // Task A: Upload images (Run in parallel)
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
                                if (session == null) throw new AppException(ErrorCode.SESSION_NOT_FOUND);

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

            // WAIT FOR ALL TO COMPLETE
            CompletableFuture<Void> allUploads = CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0]));
            CompletableFuture.allOf(allUploads, profileFuture, sessionFuture).join();

            // COLLECT RESULTS
            List<String> photoUrls = uploadFutures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            BasicProfileInfo userProfile = profileFuture.join();
            SessionInfo session = sessionFuture.join();

            log.info("I/O processing completed in {}ms", System.currentTimeMillis() - startTime);

            // AI CONTENT MODERATION
            if (request.getContent() != null && !request.getContent().isBlank()) {
                var moderationResult = contentModerationProvider.moderate(request.getContent(), "post");
                if (moderationResult.isBlocked()) {
                    log.warn("Post content blocked by AI moderation for user {}: {}", userId, moderationResult.reason());
                    throw new AppException(ErrorCode.CONTENT_MODERATION_FAILED);
                }
            }

            // SAVE TO DB
            return savePostToDb(request, userId, userProfile, photoUrls, session, type, groupId, status, isHidden);

        } catch (CompletionException e) {
            log.error("Parallel error in processAndSavePost", e);
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

        // RECIPE REVIEW DATA
        if (type == PostType.RECIPE_REVIEW) {
            if (request.getReviewRating() == null || request.getReviewRating() < 1 || request.getReviewRating() > 5) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Review rating must be between 1 and 5");
            }
            if (session == null) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Recipe review requires a cooking session");
            }
            post.setReviewRating(request.getReviewRating());
        }

        // RECIPE BATTLE DATA
        if (type == PostType.RECIPE_BATTLE) {
            if (!StringUtils.hasText(request.getBattleRecipeIdA()) || !StringUtils.hasText(request.getBattleRecipeIdB())) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Recipe battle requires two recipe IDs");
            }
            if (request.getBattleRecipeIdA().equals(request.getBattleRecipeIdB())) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Battle recipes must be different");
            }
            RecipeSummaryInfo recipeA = recipeProvider.getRecipeSummary(request.getBattleRecipeIdA());
            RecipeSummaryInfo recipeB = recipeProvider.getRecipeSummary(request.getBattleRecipeIdB());
            if (recipeA == null || recipeB == null) {
                throw new AppException(ErrorCode.RECIPE_NOT_FOUND, "One or both battle recipes not found");
            }
            post.setBattleRecipeIdA(recipeA.getId());
            post.setBattleRecipeIdB(recipeB.getId());
            post.setBattleRecipeTitleA(recipeA.getTitle());
            post.setBattleRecipeTitleB(recipeB.getTitle());
            post.setBattleRecipeImageA(recipeA.getCoverImageUrl());
            post.setBattleRecipeImageB(recipeB.getCoverImageUrl());
            post.setBattleVotesA(0);
            post.setBattleVotesB(0);
            post.setBattleEndsAt(Instant.now().plusSeconds(48 * 60 * 60)); // 48 hours
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

        // Block editing after 1 hour (Configurable by business, can be removed if needed)
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
            // Admin can delete (if role check needed), simple check here
            throw new AppException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        }

        // Cascade: delete all related data before post
        postLikeRepository.deleteAllByPostId(postId);
        postSaveRepository.deleteAllByPostId(postId);
        pollVoteRepository.deleteAllByPostId(postId);
        plateRatingRepository.deleteAllByPostId(postId);
        battleVoteRepository.deleteAllByPostId(postId);

        // Cascade comment subtree: batch delete reply likes -> replies -> comment likes -> comments
        List<Comment> comments = commentRepository.findByPostId(postId);
        if (!comments.isEmpty()) {
            List<String> commentIds = comments.stream().map(Comment::getId).collect(Collectors.toList());
            List<Reply> allReplies = replyRepository.findByParentCommentIdIn(commentIds);
            if (!allReplies.isEmpty()) {
                List<String> replyIds = allReplies.stream().map(Reply::getId).collect(Collectors.toList());
                replyLikeRepository.deleteAllByReplyIdIn(replyIds);
            }
            replyRepository.deleteAllByParentCommentIdIn(commentIds);
            commentLikeRepository.deleteAllByCommentIdIn(commentIds);
        }
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
        try {
            postLikeRepository.save(postLike);
        } catch (DuplicateKeyException e) {
            // Race condition: concurrent double-tap — already liked, return idempotent response
            log.debug("Duplicate like ignored for post {} by user {}", postId, userId);
            return PostLikeResponse.builder().isLiked(true).likeCount(post.getLikes()).build();
        }

        // Atomic increment to prevent race conditions
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(postId)),
                new Update().inc("likes", 1).set("updatedAt", Instant.now()),
                Post.class);

        if (!userId.equals(post.getUserId())) {
            sendLikeNotification(userId, post);
        }

        // Award social XP to the liker (1 XP per like)
        sendSocialXpEvent(userId, 1.0, "SOCIAL_LIKE", post.getId(), "Liked a post");

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
        // Should run Async or Fire-and-forget to not block main thread
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
        }, taskExecutor); // Reuse executor
    }

    private void sendSocialXpEvent(String userId, double amount, String source, String postId, String description) {
        try {
            XpRewardEvent xpEvent = XpRewardEvent.builder()
                    .userId(userId)
                    .amount(amount)
                    .source(source)
                    .postId(postId)
                    .description(description)
                    .build();
            kafkaTemplate.send("xp-delivery", xpEvent);
        } catch (Exception e) {
            log.error("Failed to send social XP event: userId={}, source={}, postId={}", userId, source, postId, e);
        }
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

        // Exclude GROUP posts from home/main feed (Facebook pattern: group posts only in groups)
        Criteria criteria = Criteria.where("hidden").is(false).and("postType").ne(PostType.GROUP.name());
        Query query = new Query(criteria).with(sort);
        query.skip((long) sortedPageable.getPageNumber() * sortedPageable.getPageSize());
        query.limit(sortedPageable.getPageSize());

        List<Post> posts = mongoTemplate.find(query, Post.class);
        long total = mongoTemplate.count(new Query(criteria), Post.class);

        List<PostResponse> responses = posts.stream()
                .map(postMapper::toPostResponse)
                .collect(Collectors.toList());

        Page<PostResponse> page = new PageImpl<>(responses, sortedPageable, total);
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

        // Cold start: use onboarding preferences as taste bootstrap
        if (tasteWeights.isEmpty()) {
            try {
                List<String> prefs = profileProvider.getUserPreferences(currentUserId);
                if (prefs != null && !prefs.isEmpty()) {
                    // Convert preferences to equal-weight taste profile
                    double weight = 1.0 / prefs.size();
                    Map<String, Double> bootstrapWeights = new java.util.HashMap<>();
                    for (String pref : prefs) {
                        bootstrapWeights.put(pref.toLowerCase().trim(), weight);
                    }
                    tasteWeights = bootstrapWeights;
                }
            } catch (Exception e) {
                log.debug("[FEED] Could not load preferences for cold start: {}", e.getMessage());
            }
        }

        // Still no taste signal? Pure trending fallback
        if (tasteWeights.isEmpty()) {
            return getAllPosts(1, pageable, currentUserId);
        }

        Set<String> interactedPostIds = getInteractedPostIds(currentUserId);

        // Fetch a broad candidate pool (3x page size for scoring + diversity)
        int candidatePoolSize = pageable.getPageSize() * 3;
        int skipCount = pageable.getPageNumber() * pageable.getPageSize();

        Criteria baseCriteria = Criteria.where("hidden").is(false)
                .and("userId").ne(currentUserId)
                .and("postStatus").is(PostStatus.ACTIVE.name())
                .and("postType").ne(PostType.GROUP.name()) // Exclude GROUP posts from personalized feed
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
     * 7-signal: saves (2x), likes (1x), dwell (0.75-2.5x graduated), views (0.5x),
     * comments (1.8x), creation (2.5x), search queries (0.3x per word).
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

        // Behavioral signals: views (0.5x), dwell (0.75-2.5x graduated), comments (1.8x), creation (2.5x)
        Map<String, Double> behavioralWeights = profileProvider.getBehavioralPostWeights(currentUserId);

        Set<String> allPostIds = new java.util.HashSet<>(likedPostIds);
        allPostIds.addAll(savedPostIds);
        allPostIds.addAll(behavioralWeights.keySet());

        Map<String, Double> tagWeights = new java.util.HashMap<>();

        if (!allPostIds.isEmpty()) {
            Query tagQuery = new Query(Criteria.where("_id").in(allPostIds));
            tagQuery.fields().include("tags").include("_id");
            List<Post> posts = mongoTemplate.find(tagQuery, Post.class);

            for (Post p : posts) {
                if (p.getTags() == null) continue;
                // Explicit signals: saves (2x), likes (1x)
                double explicitWeight = savedPostIds.contains(p.getId()) ? 2.0
                        : likedPostIds.contains(p.getId()) ? 1.0 : 0.0;
                // Behavioral signals from event tracking
                double behavioralWeight = behavioralWeights.getOrDefault(p.getId(), 0.0);
                double totalWeight = explicitWeight + behavioralWeight;
                if (totalWeight <= 0) continue;

                for (String tag : p.getTags()) {
                    tagWeights.merge(tag.toLowerCase().trim(), totalWeight, (a, b) -> a + b);
                }
            }
        }

        // Search query signal: each word in recent searches gets 0.3x weight as a tag
        // This captures intent even when the user didn't interact with results
        List<String> searchQueries = profileProvider.getRecentSearchQueries(currentUserId);
        for (String query : searchQueries) {
            for (String word : query.split("\\s+")) {
                String normalized = word.toLowerCase().trim();
                if (normalized.length() >= 3) { // skip short words like "a", "in"
                    tagWeights.merge(normalized, 0.3, (a, b) -> a + b);
                }
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

    /**
     * Public taste profile for a user: tag weights, cuisine distribution, top cuisines.
     * Combines the internal 5-signal taste vector with cuisine classification.
     */
    public TasteProfileResponse getTasteProfile(String userId) {
        Map<String, Double> tasteVector = buildWeightedTasteProfile(userId);

        if (tasteVector.isEmpty()) {
            return TasteProfileResponse.builder()
                    .tasteVector(Map.of())
                    .cuisineDistribution(List.of())
                    .totalInteractions(0)
                    .topCuisines(List.of())
                    .build();
        }

        // Known cuisine tags (lowercase) — classify tags into cuisines
        Map<String, String> tagToCuisine = Map.ofEntries(
                Map.entry("italian", "Italian"), Map.entry("pasta", "Italian"), Map.entry("pizza", "Italian"),
                Map.entry("risotto", "Italian"), Map.entry("tiramisu", "Italian"),
                Map.entry("asian", "Asian"), Map.entry("chinese", "Chinese"), Map.entry("japanese", "Japanese"),
                Map.entry("sushi", "Japanese"), Map.entry("ramen", "Japanese"), Map.entry("korean", "Korean"),
                Map.entry("thai", "Thai"), Map.entry("vietnamese", "Vietnamese"), Map.entry("indian", "Indian"),
                Map.entry("curry", "Indian"), Map.entry("tandoori", "Indian"), Map.entry("biryani", "Indian"),
                Map.entry("mexican", "Mexican"), Map.entry("tacos", "Mexican"), Map.entry("burrito", "Mexican"),
                Map.entry("french", "French"), Map.entry("croissant", "French"), Map.entry("souffle", "French"),
                Map.entry("mediterranean", "Mediterranean"), Map.entry("greek", "Mediterranean"),
                Map.entry("middle-eastern", "Middle Eastern"), Map.entry("lebanese", "Middle Eastern"),
                Map.entry("turkish", "Middle Eastern"), Map.entry("falafel", "Middle Eastern"),
                Map.entry("american", "American"), Map.entry("bbq", "American"), Map.entry("burger", "American"),
                Map.entry("southern", "American"), Map.entry("cajun", "American"),
                Map.entry("african", "African"), Map.entry("ethiopian", "African"),
                Map.entry("caribbean", "Caribbean"), Map.entry("jamaican", "Caribbean"),
                Map.entry("spanish", "Spanish"), Map.entry("tapas", "Spanish"), Map.entry("paella", "Spanish")
        );

        // Aggregate weights by cuisine
        Map<String, Double> cuisineWeights = new java.util.HashMap<>();
        Map<String, Integer> cuisineCounts = new java.util.HashMap<>();
        double uncategorizedWeight = 0;
        int totalCount = 0;

        for (var entry : tasteVector.entrySet()) {
            String tag = entry.getKey().toLowerCase().trim();
            double weight = entry.getValue();
            String cuisine = tagToCuisine.get(tag);
            totalCount++;
            if (cuisine != null) {
                cuisineWeights.merge(cuisine, weight, (a, b) -> a + b);
                cuisineCounts.merge(cuisine, 1, (a, b) -> a + b);
            } else {
                uncategorizedWeight += weight;
            }
        }

        // Compute percentages
        double totalWeight = cuisineWeights.values().stream().mapToDouble(Double::doubleValue).sum() + uncategorizedWeight;
        if (totalWeight <= 0) totalWeight = 1;

        double finalTotalWeight = totalWeight;
        List<TasteProfileResponse.CuisineBreakdown> distribution = cuisineWeights.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(e -> TasteProfileResponse.CuisineBreakdown.builder()
                        .cuisine(e.getKey())
                        .percentage(Math.round(e.getValue() / finalTotalWeight * 1000.0) / 10.0) // 1 decimal
                        .interactionCount(cuisineCounts.getOrDefault(e.getKey(), 0))
                        .build())
                .toList();

        // Add "Other" if significant
        if (uncategorizedWeight > 0 && distribution.size() < 10) {
            distribution = new java.util.ArrayList<>(distribution);
            distribution.add(TasteProfileResponse.CuisineBreakdown.builder()
                    .cuisine("Other")
                    .percentage(Math.round(uncategorizedWeight / finalTotalWeight * 1000.0) / 10.0)
                    .interactionCount(0)
                    .build());
        }

        List<String> topCuisines = distribution.stream()
                .limit(3)
                .map(TasteProfileResponse.CuisineBreakdown::getCuisine)
                .toList();

        return TasteProfileResponse.builder()
                .tasteVector(tasteVector)
                .cuisineDistribution(distribution)
                .totalInteractions(totalCount)
                .topCuisines(topCuisines)
                .build();
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

        // Exclude GROUP posts from user profile (Facebook pattern: group posts only in groups)
        Criteria criteria = Criteria.where("userId").is(userId)
                .and("hidden").is(false)
                .and("postType").ne(PostType.GROUP.name());

        Query query = new Query(criteria).with(Sort.by("createdAt").descending());
        query.skip((long) sortedPageable.getPageNumber() * sortedPageable.getPageSize());
        query.limit(sortedPageable.getPageSize());

        List<Post> posts = mongoTemplate.find(query, Post.class);
        long total = mongoTemplate.count(new Query(criteria), Post.class);

        List<PostResponse> responses = posts.stream()
                .map(postMapper::toPostResponse)
                .collect(Collectors.toList());

        Page<PostResponse> page = new PageImpl<>(responses, sortedPageable, total);
        enrichPageWithUserStatus(page, currentUserId);
        return page;
    }

    /**
     * Search posts by content, display name, or tags.
     * Uses case-insensitive regex matching (same pattern as RecipeSpecification).
     * Excludes GROUP posts from search results (Facebook pattern: group posts only in groups).
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
                Criteria.where("postType").ne(PostType.GROUP.name()), // Exclude GROUP posts from search
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
     * Excludes GROUP posts (Facebook pattern: group posts only displayed in groups).
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

        // Exclude GROUP posts (only show in group context)
        Sort sort = (mode == 1) ? Sort.by("hotScore").descending() : Sort.by("createdAt").descending();
        Criteria criteria = Criteria.where("userId").in(followingIds)
                .and("hidden").is(false)
                .and("postType").ne(PostType.GROUP.name());

        Query query = new Query(criteria).with(sort);
        query.skip((long) pageable.getPageNumber() * pageable.getPageSize());
        query.limit(pageable.getPageSize());

        List<Post> posts = mongoTemplate.find(query, Post.class);
        long total = mongoTemplate.count(new Query(criteria), Post.class);

        List<PostResponse> responses = posts.stream()
                .map(postMapper::toPostResponse)
                .collect(Collectors.toList());

        Page<PostResponse> page = new PageImpl<>(responses, pageable, total);
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
            pollVoteRepository.findByPostIdInAndUserId(pollPostIds, currentUserId)
                    .forEach(v -> pollVoteMap.put(v.getPostId(), v.getOption()));
        }

        // Batch battle vote lookup for battle posts
        List<String> battlePostIds = content.stream()
                .filter(p -> p.getPostType() == PostType.RECIPE_BATTLE)
                .map(PostResponse::getId)
                .collect(Collectors.toList());
        Map<String, String> battleVoteMap = new HashMap<>();
        if (!battlePostIds.isEmpty()) {
            battleVoteRepository.findByPostIdInAndUserId(battlePostIds, currentUserId)
                    .forEach(v -> battleVoteMap.put(v.getPostId(), v.getChoice()));
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
            if (post.getPostType() == PostType.RECIPE_BATTLE) {
                post.setUserBattleVote(battleVoteMap.get(post.getId()));
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
            if (post.getPostType() == PostType.RECIPE_BATTLE) {
                battleVoteRepository.findByPostIdAndUserId(post.getId(), currentUserId)
                        .ifPresent(v -> post.setUserBattleVote(v.getChoice()));
            }
            plateRatingRepository.findByPostIdAndUserId(post.getId(), currentUserId)
                    .ifPresent(pr -> post.setUserPlateRating(pr.getRating()));
        }
        return post;
    }

    // ========================================================================
    // POLL VOTING
    // ========================================================================

    @Transactional
    public PollVoteResponse votePoll(String postId, String option, String userId) {
        if (!"A".equals(option) && !"B".equals(option)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Invalid poll option: must be 'A' or 'B'");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        if (post.getPostType() != PostType.POLL || post.getPollData() == null) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "This post is not a poll");
        }

        var existingVote = pollVoteRepository.findByPostIdAndUserId(postId, userId);
        Query query = Query.query(Criteria.where("id").is(postId));

        if (existingVote.isPresent()) {
            PollVote vote = existingVote.get();
            if (vote.getOption().equals(option)) {
                // Same vote -- remove it (toggle off)
                pollVoteRepository.delete(vote);
                String field = "A".equals(option) ? "pollData.votesA" : "pollData.votesB";
                mongoTemplate.updateFirst(query, new Update().inc(field, -1), Post.class);
                Post updated = postRepository.findById(postId).orElse(post);
                PollData poll = updated.getPollData();
                return PollVoteResponse.builder()
                        .userVote(null)
                        .votesA(Math.max(0, poll.getVotesA()))
                        .votesB(Math.max(0, poll.getVotesB()))
                        .build();
            } else {
                // Switch vote
                String oldOption = vote.getOption();
                vote.setOption(option);
                pollVoteRepository.save(vote);
                String decField = "A".equals(oldOption) ? "pollData.votesA" : "pollData.votesB";
                String incField = "A".equals(option) ? "pollData.votesA" : "pollData.votesB";
                mongoTemplate.updateFirst(query, new Update().inc(decField, -1).inc(incField, 1), Post.class);
                Post updated = postRepository.findById(postId).orElse(post);
                PollData poll = updated.getPollData();
                return PollVoteResponse.builder()
                        .userVote(option)
                        .votesA(Math.max(0, poll.getVotesA()))
                        .votesB(Math.max(0, poll.getVotesB()))
                        .build();
            }
        }

        // New vote
        try {
            pollVoteRepository.save(PollVote.builder()
                    .postId(postId)
                    .userId(userId)
                    .option(option)
                    .build());
        } catch (DuplicateKeyException e) {
            // Race condition: concurrent vote arrived first — retry as toggle
            return votePoll(postId, option, userId);
        }
        String incField = "A".equals(option) ? "pollData.votesA" : "pollData.votesB";
        mongoTemplate.updateFirst(query, new Update().inc(incField, 1), Post.class);
        Post updated = postRepository.findById(postId).orElse(post);
        PollData poll = updated.getPollData();

        return PollVoteResponse.builder()
                .userVote(option)
                .votesA(poll.getVotesA())
                .votesB(poll.getVotesB())
                .build();
    }

    @Transactional
    public PlateRateResponse ratePlate(String postId, String rating, String userId) {
        if (!"FIRE".equals(rating) && !"CRINGE".equals(rating)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Invalid plate rating: must be 'FIRE' or 'CRINGE'");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        var existing = plateRatingRepository.findByPostIdAndUserId(postId, userId);
        Query query = Query.query(Criteria.where("id").is(postId));

        if (existing.isPresent()) {
            PlateRating pr = existing.get();
            if (pr.getRating().equals(rating)) {
                // Same rating -- toggle off
                plateRatingRepository.delete(pr);
                String field = "FIRE".equals(rating) ? "fireCount" : "cringeCount";
                mongoTemplate.updateFirst(query, new Update().inc(field, -1), Post.class);
                Post updated = postRepository.findById(postId).orElse(post);
                return PlateRateResponse.builder()
                        .userRating(null)
                        .fireCount(Math.max(0, updated.getFireCount()))
                        .cringeCount(Math.max(0, updated.getCringeCount()))
                        .build();
            } else {
                // Switch rating
                String old = pr.getRating();
                pr.setRating(rating);
                plateRatingRepository.save(pr);
                String decField = "FIRE".equals(old) ? "fireCount" : "cringeCount";
                String incField = "FIRE".equals(rating) ? "fireCount" : "cringeCount";
                mongoTemplate.updateFirst(query, new Update().inc(decField, -1).inc(incField, 1), Post.class);
                Post updated = postRepository.findById(postId).orElse(post);
                return PlateRateResponse.builder()
                        .userRating(rating)
                        .fireCount(Math.max(0, updated.getFireCount()))
                        .cringeCount(Math.max(0, updated.getCringeCount()))
                        .build();
            }
        }

        // New rating
        try {
            plateRatingRepository.save(PlateRating.builder()
                    .postId(postId)
                    .userId(userId)
                    .rating(rating)
                    .build());
        } catch (DuplicateKeyException e) {
            // Race condition: concurrent rating arrived first — retry as toggle
            return ratePlate(postId, rating, userId);
        }
        String incField = "FIRE".equals(rating) ? "fireCount" : "cringeCount";
        mongoTemplate.updateFirst(query, new Update().inc(incField, 1), Post.class);
        Post updated = postRepository.findById(postId).orElse(post);

        return PlateRateResponse.builder()
                .userRating(rating)
                .fireCount(updated.getFireCount())
                .cringeCount(updated.getCringeCount())
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
        try {
            postSaveRepository.save(postSave);
        } catch (DuplicateKeyException e) {
            // Race condition: concurrent double-tap — already saved, return idempotent response
            log.debug("Duplicate save ignored for post {} by user {}", postId, userId);
            long saveCount = postSaveRepository.countByPostId(postId);
            return PostSaveResponse.builder().isSaved(true).saveCount(saveCount).build();
        }

        // Award social XP to the saver (1 XP per save)
        sendSocialXpEvent(userId, 1.0, "SOCIAL_SAVE", postId, "Saved a post");
        
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

    // ========================================================================
    // RECIPE REVIEWS
    // ========================================================================

    /**
     * Get all reviews for a specific recipe, newest first.
     */
    public Page<PostResponse> getReviewsForRecipe(String recipeId, Pageable pageable, String currentUserId) {
        Page<Post> posts = postRepository.findByRecipeIdAndPostTypeAndHiddenFalseOrderByCreatedAtDesc(
                recipeId, PostType.RECIPE_REVIEW, pageable);
        Page<PostResponse> responses = posts.map(postMapper::toPostResponse);
        enrichPageWithUserStatus(responses, currentUserId);
        return responses;
    }

    /**
     * Get aggregate review stats for a recipe (average rating, total count).
     */
    public RecipeReviewStatsResponse getRecipeReviewStats(String recipeId) {
        // Use MongoTemplate for aggregation (avg rating)
        var matchCriteria = Criteria.where("recipeId").is(recipeId)
                .and("postType").is(PostType.RECIPE_REVIEW)
                .and("hidden").is(false)
                .and("reviewRating").ne(null);

        Query countQuery = Query.query(matchCriteria);
        long totalReviews = mongoTemplate.count(countQuery, Post.class);

        if (totalReviews == 0) {
            return RecipeReviewStatsResponse.builder()
                    .recipeId(recipeId)
                    .averageRating(0.0)
                    .totalReviews(0)
                    .build();
        }

        // Use Spring Data MongoDB aggregation for average
        var aggregation = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                org.springframework.data.mongodb.core.aggregation.Aggregation.match(matchCriteria),
                org.springframework.data.mongodb.core.aggregation.Aggregation.group()
                        .avg("reviewRating").as("avgRating")
                        .count().as("count")
        );

        var results = mongoTemplate.aggregate(aggregation, "post", java.util.Map.class);
        var firstResult = results.getUniqueMappedResult();

        double avgRating = 0.0;
        if (firstResult != null && firstResult.get("avgRating") != null) {
            avgRating = ((Number) firstResult.get("avgRating")).doubleValue();
            avgRating = Math.round(avgRating * 10.0) / 10.0; // Round to 1 decimal
        }

        return RecipeReviewStatsResponse.builder()
                .recipeId(recipeId)
                .averageRating(avgRating)
                .totalReviews(totalReviews)
                .build();
    }

    // ========================================================================
    // RECIPE BATTLES
    // ========================================================================

    /**
     * Vote in a recipe battle. One vote per user; toggle removes vote.
     */
    @Transactional
    public BattleVoteResponse voteBattle(String postId, String choice) {
        if (!"A".equals(choice) && !"B".equals(choice)) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Battle vote must be 'A' or 'B'");
        }

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        if (post.getPostType() != PostType.RECIPE_BATTLE) {
            throw new AppException(ErrorCode.INVALID_INPUT, "This post is not a recipe battle");
        }

        if (post.getBattleEndsAt() != null && Instant.now().isAfter(post.getBattleEndsAt())) {
            throw new AppException(ErrorCode.INVALID_INPUT, "This battle has ended");
        }

        Query query = Query.query(Criteria.where("_id").is(postId));
        Optional<BattleVote> existingVote = battleVoteRepository.findByPostIdAndUserId(postId, userId);

        if (existingVote.isPresent()) {
            String oldChoice = existingVote.get().getChoice();
            if (oldChoice.equals(choice)) {
                // Toggle off — remove vote
                battleVoteRepository.delete(existingVote.get());
                String decField = "A".equals(oldChoice) ? "battleVotesA" : "battleVotesB";
                mongoTemplate.updateFirst(query, new Update().inc(decField, -1), Post.class);
                Post updated = postRepository.findById(postId).orElse(post);
                return BattleVoteResponse.builder()
                        .userVote(null)
                        .votesA(Math.max(0, updated.getBattleVotesA()))
                        .votesB(Math.max(0, updated.getBattleVotesB()))
                        .build();
            } else {
                // Switch vote
                existingVote.get().setChoice(choice);
                battleVoteRepository.save(existingVote.get());
                String decField = "A".equals(oldChoice) ? "battleVotesA" : "battleVotesB";
                String incField = "A".equals(choice) ? "battleVotesA" : "battleVotesB";
                mongoTemplate.updateFirst(query, new Update().inc(decField, -1).inc(incField, 1), Post.class);
                Post updated = postRepository.findById(postId).orElse(post);
                return BattleVoteResponse.builder()
                        .userVote(choice)
                        .votesA(Math.max(0, updated.getBattleVotesA()))
                        .votesB(Math.max(0, updated.getBattleVotesB()))
                        .build();
            }
        }

        // New vote
        try {
            battleVoteRepository.save(BattleVote.builder()
                    .postId(postId)
                    .userId(userId)
                    .choice(choice)
                    .build());
        } catch (DuplicateKeyException e) {
            // Race condition: concurrent vote arrived first — retry as toggle
            return voteBattle(postId, choice);
        }
        String incField = "A".equals(choice) ? "battleVotesA" : "battleVotesB";
        mongoTemplate.updateFirst(query, new Update().inc(incField, 1), Post.class);
        Post updated = postRepository.findById(postId).orElse(post);

        return BattleVoteResponse.builder()
                .userVote(choice)
                .votesA(updated.getBattleVotesA())
                .votesB(updated.getBattleVotesB())
                .build();
    }

    /**
     * Get active recipe battles (not yet ended), ordered by ending soonest.
     */
    public Page<PostResponse> getActiveBattles(Pageable pageable, String currentUserId) {
        Page<Post> posts = postRepository.findByPostTypeAndBattleEndsAtAfterAndHiddenFalseOrderByBattleEndsAtAsc(
                PostType.RECIPE_BATTLE, Instant.now(), pageable);
        Page<PostResponse> responses = posts.map(postMapper::toPostResponse);
        enrichPageWithUserStatus(responses, currentUserId);
        return responses;
    }
}