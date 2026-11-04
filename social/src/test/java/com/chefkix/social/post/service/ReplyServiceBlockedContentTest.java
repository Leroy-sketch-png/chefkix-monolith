package com.chefkix.social.post.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.api.ContentModerationProvider;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.post.entity.Comment;
import com.chefkix.social.post.dto.request.ReplyRequest;
import com.chefkix.social.post.dto.response.ReplyResponse;
import com.chefkix.social.post.entity.Reply;
import com.chefkix.social.post.mapper.ReplyMapper;
import com.chefkix.social.policy.BlockedContentPolicy;
import com.chefkix.social.post.repository.CommentRepository;
import com.chefkix.social.post.repository.PostRepository;
import com.chefkix.social.post.repository.ReplyLikeRepository;
import com.chefkix.social.post.repository.ReplyRepository;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ReplyServiceBlockedContentTest {

    @Test
    void replyFeedOmitsAuthorsInvisibleToViewer() {
        CommentRepository comments = mock(CommentRepository.class);
        ReplyRepository replies = mock(ReplyRepository.class);
        ReplyMapper mapper = mock(ReplyMapper.class);
        BlockedContentPolicy blockedContent = mock(BlockedContentPolicy.class);
        ReplyService service = new ReplyService(
                comments,
                mock(PostRepository.class),
                replies,
                mock(ReplyLikeRepository.class),
                mapper,
                mock(ProfileProvider.class),
                mock(ContentModerationProvider.class),
                mock(MongoTemplate.class),
                mock(KafkaTemplate.class),
                blockedContent);

        Comment parent = mock(Comment.class);
        Reply blocked = mock(Reply.class);
        Reply visible = mock(Reply.class);
        when(comments.findById("comment-1")).thenReturn(Optional.of(parent));
        when(parent.getUserId()).thenReturn("parent-author");
        when(replies.findByParentCommentIdAndUserIdNotIn(
                org.mockito.ArgumentMatchers.eq("comment-1"),
                org.mockito.ArgumentMatchers.eq(List.of("blocked-author")),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(visible));
        when(blockedContent.invisibleUserIds("viewer")).thenReturn(List.of("blocked-author"));
        when(blocked.getUserId()).thenReturn("blocked-author");
        when(visible.getUserId()).thenReturn("other-author");
        when(visible.getId()).thenReturn("visible-reply");
        when(mapper.toResponse(visible)).thenReturn(new ReplyResponse());

        List<ReplyResponse> result = service.getAllRepliesByCommentId("comment-1", "viewer");

        org.junit.jupiter.api.Assertions.assertEquals(1, result.size());
        verify(replies, never()).findByParentCommentId(
                org.mockito.ArgumentMatchers.eq("comment-1"), org.mockito.ArgumentMatchers.any());
        verify(replies).findByParentCommentIdAndUserIdNotIn(
                org.mockito.ArgumentMatchers.eq("comment-1"),
                org.mockito.ArgumentMatchers.eq(List.of("blocked-author")),
                org.mockito.ArgumentMatchers.any());
        verify(mapper, never()).toResponse(blocked);
        verify(mapper).toResponse(visible);
    }

    @Test
    void createReplyRejectsBlockedParentBeforeProfileLookupOrModeration() {
        CommentRepository comments = mock(CommentRepository.class);
        ProfileProvider profiles = mock(ProfileProvider.class);
        ContentModerationProvider moderation = mock(ContentModerationProvider.class);
        BlockedContentPolicy blockedContent = mock(BlockedContentPolicy.class);
        ReplyRepository replies = mock(ReplyRepository.class);
        ReplyService service = new ReplyService(
                comments,
                mock(PostRepository.class),
                replies,
                mock(ReplyLikeRepository.class),
                mock(ReplyMapper.class),
                profiles,
                moderation,
                mock(MongoTemplate.class),
                mock(KafkaTemplate.class),
                blockedContent);

        Comment parent = mock(Comment.class);
        when(parent.getUserId()).thenReturn("blocked-author");
        when(comments.findById("comment-1")).thenReturn(Optional.of(parent));
        doThrow(new AppException(ErrorCode.POST_NOT_FOUND))
                .when(blockedContent).requireVisible("viewer", "blocked-author");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("viewer", "n/a"));
        try {
            assertThrows(AppException.class, () -> service.createReply(
                    ReplyRequest.builder().parentCommentId("comment-1").content("reply").build()));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(profiles, never()).getBasicProfile("viewer");
        verify(moderation, never()).moderate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(replies, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blockedReplyLikeRejectsBeforePersistenceOrCounterUpdate() {
        ReplyRepository replies = mock(ReplyRepository.class);
        ReplyLikeRepository likes = mock(ReplyLikeRepository.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
        BlockedContentPolicy blockedContent = mock(BlockedContentPolicy.class);
        ReplyService service = new ReplyService(
                mock(CommentRepository.class),
                mock(PostRepository.class),
                replies,
                likes,
                mock(ReplyMapper.class),
                mock(ProfileProvider.class),
                mock(ContentModerationProvider.class),
                mongoTemplate,
                kafka,
                blockedContent);

        var authentication = new UsernamePasswordAuthenticationToken("viewer", "n/a");
        Reply reply = mock(Reply.class);
        when(reply.getUserId()).thenReturn("blocked-author");
        when(replies.findById("reply-1")).thenReturn(Optional.of(reply));
        doThrow(new AppException(ErrorCode.POST_NOT_FOUND))
                .when(blockedContent).requireVisible("viewer", "blocked-author");

        assertThrows(AppException.class, () -> service.toggleLike(authentication, "reply-1"));

        verify(likes, never()).save(org.mockito.ArgumentMatchers.any());
        verify(likes, never()).deleteByReplyIdAndUserId(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        verify(mongoTemplate, never()).updateFirst(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(Comment.class));
        verify(kafka, never()).send(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
