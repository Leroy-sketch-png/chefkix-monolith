package com.chefkix.social.post.service;

import com.chefkix.culinary.api.ContentModerationProvider;
import com.chefkix.shared.event.CommentEvent;
import com.chefkix.shared.event.UserMentionEvent;
import com.chefkix.social.post.dto.request.CommentRequest;
import com.chefkix.social.post.dto.response.CommentLikeResponse;
import com.chefkix.social.post.dto.response.CommentResponse;
import com.chefkix.social.post.entity.Comment;
import com.chefkix.social.post.entity.CommentLike;
import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.entity.TaggedUserInfo;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.post.mapper.CommentMapper;
import com.chefkix.social.post.entity.Reply;
import com.chefkix.social.post.repository.CommentLikeRepository;
import com.chefkix.social.post.repository.CommentRepository;
import com.chefkix.social.post.repository.PostRepository;
import com.chefkix.social.post.repository.ReplyLikeRepository;
import com.chefkix.social.post.repository.ReplyRepository;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentService {
  CommentRepository commentRepository;
  PostRepository postRepository;
  CommentLikeRepository commentLikeRepository;
  ReplyRepository replyRepository;
  ReplyLikeRepository replyLikeRepository;
  KafkaTemplate<String, Object> kafkaTemplate;
    // Regex bắt format: @[userId|displayName
    static Pattern TAG_PATTERN = Pattern.compile("@\\[([^|]+)\\|([^]]+)\\]");
  CommentMapper commentMapper;
    private final ProfileProvider profileProvider;
    private final ContentModerationProvider contentModerationProvider;
    private final MongoTemplate mongoTemplate;

    public CommentResponse createComment(
      Authentication authentication, String postId, CommentRequest req) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    String userId = authentication.getName();

    // Check to see if post is there
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        BasicProfileInfo userInfo = profileProvider.getBasicProfile(userId);
        if (userInfo == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        // Extract tagged user IDs from:
        // 1. Explicit taggedUserIds from request (new clean approach)
        // 2. Legacy @[userId|displayName] format in content (backward compat)
        List<String> extractedTagIds = new ArrayList<>();
        
        // Add explicit taggedUserIds from request
        if (req.getTaggedUserIds() != null && !req.getTaggedUserIds().isEmpty()) {
            extractedTagIds.addAll(req.getTaggedUserIds());
        }
        
        // Also parse legacy format for backward compatibility
        Matcher matcher = TAG_PATTERN.matcher(req.getContent());
        while (matcher.find()) {
            String legacyTagId = matcher.group(1);
            if (!extractedTagIds.contains(legacyTagId)) {
                extractedTagIds.add(legacyTagId);
            }
        }

        // AI CONTENT MODERATION — fail-open for comments
        var moderationResult = contentModerationProvider.moderate(req.getContent(), "comment");
        if (moderationResult.isBlocked()) {
            log.warn("Comment content blocked by AI moderation for user {}: {}", userId, moderationResult.reason());
            throw new AppException(ErrorCode.CONTENT_MODERATION_FAILED);
        }

    Comment comment =
        Comment.builder()
            .userId(userId)
            .postId(postId)
            .likes(0)
            .replyCount(0)
            .displayName(userInfo.getDisplayName())
            .avatarUrl(userInfo.getAvatarUrl())
            .content(req.getContent())
                .taggedUserIds(extractedTagIds)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    commentRepository.save(comment);

    // Atomic increment to prevent race conditions
    mongoTemplate.updateFirst(
            Query.query(Criteria.where("id").is(postId)),
            new Update().inc("commentCount", 1),
            Post.class);

    // Send notification to post owner (only if commenter is not the post owner)
    if (!userId.equals(post.getUserId())) {
        sendCommentNotification(comment, post, userInfo.getDisplayName(), userInfo.getAvatarUrl());
    }

    // Send notification to the tagged users
    if (!extractedTagIds.isEmpty()) {
        sendTagNotification(comment, post, extractedTagIds);
    }

    return commentMapper.toCommentResponse(comment);
  }

  /**
   * Sends Kafka event to notify post owner of new comment.
   */
  private void sendCommentNotification(Comment comment, Post post, String displayName, String avatarUrl) {
      try {
          String contentPreview = comment.getContent();
          if (contentPreview != null && contentPreview.length() > 50) {
              contentPreview = contentPreview.substring(0, 50) + "...";
          }

          CommentEvent event = CommentEvent.builder()
                  .postId(post.getId())
                  .commentId(comment.getId())
                  .commenterId(comment.getUserId())
                  .commenterDisplayName(displayName)
                  .commenterAvatarUrl(avatarUrl)
                  .postOwnerId(post.getUserId())
                  .contentPreview(contentPreview)
                  .build();

          kafkaTemplate.send("comment-delivery", event);
          log.debug("Comment notification sent for comment {} on post {}", comment.getId(), post.getId());
      } catch (Exception e) {
          log.error("Failed to send comment notification", e);
      }
  }

    /**
     * Sends Kafka event to notify tagged user.
     */
    private void sendTagNotification(Comment comment, Post post, List<String> taggedUserIds) {
        String actorDisplayName = comment.getDisplayName();
        String actorAvatarUrl = comment.getAvatarUrl();

        // Loop qua từng người được tag để gửi event riêng biệt
        for (String taggedUserId : taggedUserIds) {
            // Bỏ qua nếu tự tag chính mình (tránh spam)
            if (taggedUserId.equals(comment.getUserId())) continue;

            try {
                UserMentionEvent event = UserMentionEvent.builder()
                        .recipientId(taggedUserId)      // Gửi cho người được tag
                        .sourceId(comment.getId())
                        .sourceType("COMMENT")
                        .postId(post.getId())
                        .actorId(comment.getUserId())   // Người tag
                        .actorDisplayName(actorDisplayName)
                        .actorAvatarUrl(actorAvatarUrl)
                        .contentPreview(getPreview(comment.getContent()))
                        .build();

                kafkaTemplate.send("tag-delivery", event);

            } catch (Exception e) {
                log.error("Failed to send tag notification for comment {} to user {}", comment.getId(), taggedUserId, e);
            }
        }
    }

    private String getPreview(String content) {
        return (content != null && content.length() > 50)
                ? content.substring(0, 50) + "..."
                : content;
    }

  public int getCurrentReplyCount(String commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        return comment.getReplyCount();
  }

    public List<CommentResponse> getAllCommentsByPostId(String postId, String currentUserId) {
        List<Comment> comments = commentRepository.findByPostId(postId,
                PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<String, BasicProfileInfo> taggedProfileCache = new HashMap<>();

        // 2. Chuyển đổi và "làm giàu" (enrich) từng comment
        return comments.stream()
            .map(comment -> mapToCommentResponse(comment, currentUserId, taggedProfileCache))
                .collect(Collectors.toList());
    }

        private CommentResponse mapToCommentResponse(
            Comment comment,
            String currentUserId,
            Map<String, BasicProfileInfo> taggedProfileCache) {
        // 1. Dùng mapper để map các trường cơ bản
        CommentResponse response = commentMapper.toCommentResponse(comment);
        
        // 2. Check if current user has liked this comment
        if (currentUserId != null) {
            boolean isLiked = commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), currentUserId);
            response.setIsLiked(isLiked);
        } else {
            response.setIsLiked(false);
        }

        // 3. Khởi tạo list
        if (response.getTaggedUsers() == null) {
            response.setTaggedUsers(new ArrayList<>());
        }

        List<String> taggedIds = comment.getTaggedUserIds();

        // 4. Logic "làm giàu": Lặp qua các ID và gọi ProfileClient
        if (taggedIds != null && !taggedIds.isEmpty()) {
            for (String taggedUserId : taggedIds) {
                try {
                    BasicProfileInfo taggedUserInfo = taggedProfileCache.get(taggedUserId);
                    if (taggedUserInfo == null) {
                        taggedUserInfo = profileProvider.getBasicProfile(taggedUserId);
                        if (taggedUserInfo != null) {
                            taggedProfileCache.put(taggedUserId, taggedUserInfo);
                        }
                    }
                    if (taggedUserInfo == null) {
                        continue;
                    }

                    TaggedUserInfo taggedUserInCmt = new TaggedUserInfo();
                    taggedUserInCmt.setDisplayName(taggedUserInfo.getDisplayName());
                    taggedUserInCmt.setUserId(taggedUserId);

                    response.getTaggedUsers().add(taggedUserInCmt);
                } catch (Exception e) {
                    log.warn("Could not resolve tagged user {}", taggedUserId, e);
                }
            }
        }

        return response;
    }

    /**
     * Delete a comment. Only the comment owner can delete their comment.
     */
    @Transactional
    public void deleteComment(Authentication authentication, String postId, String commentId) {
        String userId = authentication.getName();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        // Verify ownership
        if (!comment.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Verify comment belongs to the post
        if (!comment.getPostId().equals(postId)) {
            throw new AppException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // Delete all reply likes for replies on this comment
        List<Reply> replies = replyRepository.findByParentCommentId(commentId);
        for (Reply reply : replies) {
            replyLikeRepository.deleteAllByReplyId(reply.getId());
        }

        // Delete all replies to this comment
        replyRepository.deleteAllByParentCommentId(commentId);

        // Delete all likes on this comment
        commentLikeRepository.deleteAllByCommentId(commentId);

        // Delete the comment
        commentRepository.delete(comment);

        // Atomically decrement comment count on post
        Query postQuery = Query.query(Criteria.where("id").is(postId));
        Update postUpdate = new Update().inc("commentCount", -1);
        mongoTemplate.updateFirst(postQuery, postUpdate, Post.class);

        log.info("Comment {} deleted by user {}", commentId, userId);
    }

    /**
     * Toggle like on a comment. Returns new like state and count.
     */
    @Transactional
    public CommentLikeResponse toggleLike(Authentication authentication, String commentId) {
        String userId = authentication.getName();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        boolean alreadyLiked = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);

        if (alreadyLiked) {
            // Unlike
            commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
            incrementCommentLikes(commentId, -1);
        } else {
            // Like
            CommentLike like = CommentLike.builder()
                    .commentId(commentId)
                    .userId(userId)
                    .createdAt(Instant.now())
                    .build();
            commentLikeRepository.save(like);
            incrementCommentLikes(commentId, 1);
        }

        long likeCount = commentLikeRepository.countByCommentId(commentId);
        return CommentLikeResponse.builder()
                .isLiked(!alreadyLiked)
                .likes((int) likeCount)
                .build();
    }

    private void incrementCommentLikes(String commentId, int amount) {
        Query query = Query.query(Criteria.where("id").is(commentId));
        Update update = new Update().inc("likes", amount);
        mongoTemplate.updateFirst(query, update, Comment.class);
    }
}
