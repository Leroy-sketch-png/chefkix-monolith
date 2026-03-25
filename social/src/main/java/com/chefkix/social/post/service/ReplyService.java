package com.chefkix.social.post.service;

import com.chefkix.culinary.api.ContentModerationProvider;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.shared.event.UserMentionEvent;
import com.chefkix.social.post.dto.request.ReplyRequest;
import com.chefkix.social.post.dto.response.ReplyLikeResponse;
import com.chefkix.social.post.dto.response.ReplyResponse;
import com.chefkix.social.post.entity.Comment;
import com.chefkix.social.post.entity.Reply;
import com.chefkix.social.post.entity.ReplyLike;
import com.chefkix.social.post.entity.TaggedUserInfo;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.post.mapper.ReplyMapper;
import com.chefkix.social.post.repository.CommentRepository;
import com.chefkix.social.post.repository.ReplyLikeRepository;
import com.chefkix.social.post.repository.ReplyRepository;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReplyService {

    CommentRepository commentRepository;
    ReplyRepository replyRepository;
    ReplyLikeRepository replyLikeRepository;
    ReplyMapper replyMapper;
    ProfileProvider profileProvider;
    ContentModerationProvider contentModerationProvider;
    MongoTemplate mongoTemplate;
    KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public ReplyResponse createReply(ReplyRequest replyRequest) {

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        BasicProfileInfo userInfo = profileProvider.getBasicProfile(userId);
        if (userInfo == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        Comment comment = commentRepository.findById(replyRequest.getParentCommentId())
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        // AI CONTENT MODERATION — fail-open for replies
        var moderationResult = contentModerationProvider.moderate(replyRequest.getContent(), "comment");
        if (moderationResult.isBlocked()) {
            log.warn("Reply content blocked by AI moderation for user {}: {}", userId, moderationResult.reason());
            throw new AppException(ErrorCode.CONTENT_MODERATION_FAILED);
        }

        Reply reply = replyMapper.toReply(replyRequest);
        String parentCommentAuthorId = comment.getUserId();

        if (!userId.equals(parentCommentAuthorId)) {
            String originalContent = reply.getContent();
            String displayName = comment.getDisplayName();
            String newContent = "@" + displayName + " " + originalContent;
            reply.setContent(newContent);

            if (reply.getTaggedUserIds() == null) {
                reply.setTaggedUserIds(new ArrayList<>());
            }
            if (!reply.getTaggedUserIds().contains(parentCommentAuthorId)) {
                reply.getTaggedUserIds().add(parentCommentAuthorId);
            }
        }

        reply.setUserId(userId);
        reply.setDisplayName(userInfo.getDisplayName());
        reply.setAvatarUrl(userInfo.getAvatarUrl());
        reply.setLikes(0);
        reply.setCreatedAt(Instant.now());
        Reply savedReply = replyRepository.save(reply);

        incrementCounter(savedReply.getParentCommentId(), "replyCount", 1);

        // Send notification to tagged users
        if (savedReply.getTaggedUserIds() != null && !savedReply.getTaggedUserIds().isEmpty()) {
            sendTagNotifications(savedReply, comment.getPostId(), userInfo.getDisplayName(), userInfo.getAvatarUrl());
        }

        // For newly created replies, current user is the author, so isLiked = false
        return mapToReplyResponse(savedReply, userId);
    }

    public List<ReplyResponse> getAllRepliesByCommentId(String commentId, String currentUserId) {
        List<Reply> replies = replyRepository.findByParentCommentId(commentId,
                PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "createdAt")));

        return replies.stream()
                .map(reply -> mapToReplyResponse(reply, currentUserId))
                .collect(Collectors.toList());
    }

    private ReplyResponse mapToReplyResponse(Reply reply, String currentUserId) {
        ReplyResponse response = replyMapper.toResponse(reply);
        
        // Check if current user has liked this reply
        if (currentUserId != null) {
            boolean isLiked = replyLikeRepository.existsByReplyIdAndUserId(reply.getId(), currentUserId);
            response.setIsLiked(isLiked);
        } else {
            response.setIsLiked(false);
        }

        if (response.getTaggedUsers() == null) {
            response.setTaggedUsers(new ArrayList<>());
        }

        List<String> taggedIds = reply.getTaggedUserIds();

        if (taggedIds != null && !taggedIds.isEmpty()) {
            for (String taggedUserId : taggedIds) {
                try {
                    BasicProfileInfo taggedUserInfo = profileProvider.getBasicProfile(taggedUserId);
                    if (taggedUserInfo == null) {
                        log.warn("Tagged user not found: {}. Skipping.", taggedUserId);
                        continue;
                    }

                    TaggedUserInfo taggedUserInCmt = new TaggedUserInfo();
                    taggedUserInCmt.setDisplayName(taggedUserInfo.getDisplayName());
                    taggedUserInCmt.setUserId(taggedUserId);

                    response.getTaggedUsers().add(taggedUserInCmt);
                } catch (Exception e) {
                    log.warn("Could not resolve tagged user {}", taggedUserId);
                }
            }
        }

        return response;
    }

    /**
     * Sends Kafka event to notify tagged users in a reply.
     */
    private void sendTagNotifications(Reply reply, String postId, String actorDisplayName, String actorAvatarUrl) {
        for (String taggedUserId : reply.getTaggedUserIds()) {
            // Skip self-tagging
            if (taggedUserId.equals(reply.getUserId())) continue;

            try {
                UserMentionEvent event = UserMentionEvent.builder()
                        .recipientId(taggedUserId)
                        .sourceId(reply.getId())
                        .sourceType("REPLY")
                        .postId(postId)
                        .actorId(reply.getUserId())
                        .actorDisplayName(actorDisplayName)
                        .actorAvatarUrl(actorAvatarUrl)
                        .contentPreview(getPreview(reply.getContent()))
                        .build();

                kafkaTemplate.send("tag-delivery", event);
                log.info("Sent tag notification for reply {} to user {}", reply.getId(), taggedUserId);

            } catch (Exception e) {
                log.error("Failed to send reply tag notification to user {}", taggedUserId, e);
            }
        }
    }

    private String getPreview(String content) {
        return (content != null && content.length() > 50)
                ? content.substring(0, 50) + "..."
                : content;
    }

    public void incrementCounter(String commentId, String fieldName, int amount) {
        Query query = Query.query(Criteria.where("id").is(commentId));
        Update update = new Update().inc(fieldName, amount);
        mongoTemplate.updateFirst(query, update, Comment.class);
    }

    /**
     * Delete a reply. Only the reply owner can delete.
     */
    @Transactional
    public void deleteReply(Authentication authentication, String replyId) {
        String userId = authentication.getName();

        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new AppException(ErrorCode.REPLY_NOT_FOUND));

        // Only owner can delete
        if (!reply.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Decrement reply count on parent comment
        incrementCounter(reply.getParentCommentId(), "replyCount", -1);

        // Delete all likes for this reply
        replyLikeRepository.deleteAllByReplyId(replyId);

        // Delete the reply
        replyRepository.delete(reply);

        log.info("User {} deleted reply {}", userId, replyId);
    }

    /**
     * Toggle like on a reply.
     */
    @Transactional
    public ReplyLikeResponse toggleLike(Authentication authentication, String replyId) {
        String userId = authentication.getName();

        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new AppException(ErrorCode.REPLY_NOT_FOUND));

        boolean alreadyLiked = replyLikeRepository.existsByReplyIdAndUserId(replyId, userId);

        if (alreadyLiked) {
            // Unlike
            replyLikeRepository.deleteByReplyIdAndUserId(replyId, userId);
            incrementReplyLikes(replyId, -1);
        } else {
            // Like
            ReplyLike like = ReplyLike.builder()
                    .replyId(replyId)
                    .userId(userId)
                    .build();
            replyLikeRepository.save(like);
            incrementReplyLikes(replyId, 1);
        }

        long likeCount = replyLikeRepository.countByReplyId(replyId);
        return ReplyLikeResponse.builder()
                .isLiked(!alreadyLiked)
                .likes(likeCount)
                .build();
    }

    private void incrementReplyLikes(String replyId, int amount) {
        Query query = Query.query(Criteria.where("id").is(replyId));
        Update update = new Update().inc("likes", amount);
        mongoTemplate.updateFirst(query, update, Reply.class);
    }
}