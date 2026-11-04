package com.chefkix.social.post.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.api.ContentModerationProvider;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.entity.Comment;
import com.chefkix.social.post.dto.response.CommentResponse;
import com.chefkix.social.post.mapper.CommentMapper;
import com.chefkix.social.policy.BlockedContentPolicy;
import com.chefkix.social.post.repository.CommentLikeRepository;
import com.chefkix.social.post.repository.CommentRepository;
import com.chefkix.social.post.repository.PostRepository;
import com.chefkix.social.post.repository.ReplyLikeRepository;
import com.chefkix.social.post.repository.ReplyRepository;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;

class CommentServiceBlockedContentTest {

    @Test
    void commentFeedOmitsAuthorsInvisibleToViewer() {
        CommentRepository comments = mock(CommentRepository.class);
        PostRepository posts = mock(PostRepository.class);
        CommentMapper mapper = mock(CommentMapper.class);
        BlockedContentPolicy blockedContent = mock(BlockedContentPolicy.class);
        CommentService service = new CommentService(
                comments,
                posts,
                mock(CommentLikeRepository.class),
                mock(ReplyRepository.class),
                mock(ReplyLikeRepository.class),
                mock(KafkaTemplate.class),
                mapper,
                mock(ProfileProvider.class),
                mock(ContentModerationProvider.class),
                mock(MongoTemplate.class),
                blockedContent);

        Comment blocked = mock(Comment.class);
        Comment visible = mock(Comment.class);
        when(blocked.getUserId()).thenReturn("blocked-author");
        when(visible.getUserId()).thenReturn("other-author");
        when(visible.getId()).thenReturn("visible-comment");
        when(comments.findByPostIdAndUserIdNotIn(
                org.mockito.ArgumentMatchers.eq("post-1"),
                org.mockito.ArgumentMatchers.eq(List.of("blocked-author")),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(visible));
        when(posts.findById("post-1")).thenReturn(Optional.empty());
        when(blockedContent.invisibleUserIds("viewer")).thenReturn(List.of("blocked-author"));
        when(mapper.toCommentResponse(visible)).thenReturn(new CommentResponse());

        List<CommentResponse> result = service.getAllCommentsByPostId("post-1", "viewer");

        org.junit.jupiter.api.Assertions.assertEquals(1, result.size());
        verify(comments, never()).findByPostId(
                org.mockito.ArgumentMatchers.eq("post-1"), org.mockito.ArgumentMatchers.any());
        verify(comments).findByPostIdAndUserIdNotIn(
                org.mockito.ArgumentMatchers.eq("post-1"),
                org.mockito.ArgumentMatchers.eq(List.of("blocked-author")),
                org.mockito.ArgumentMatchers.any());
        verify(mapper, never()).toCommentResponse(blocked);
        verify(mapper).toCommentResponse(visible);
    }

    @Test
    void createCommentRejectsBlockedPostBeforeModerationOrPersistence() {
        CommentRepository comments = mock(CommentRepository.class);
        PostRepository posts = mock(PostRepository.class);
        ContentModerationProvider moderation = mock(ContentModerationProvider.class);
        BlockedContentPolicy blockedContent = mock(BlockedContentPolicy.class);
        CommentService service = new CommentService(
                comments,
                posts,
                mock(CommentLikeRepository.class),
                mock(ReplyRepository.class),
                mock(ReplyLikeRepository.class),
                mock(KafkaTemplate.class),
                mock(CommentMapper.class),
                mock(ProfileProvider.class),
                moderation,
                mock(MongoTemplate.class),
                blockedContent);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("viewer");
        Post post = mock(Post.class);
        when(post.getUserId()).thenReturn("blocked-author");
        when(posts.findById("post-1")).thenReturn(Optional.of(post));
        doThrow(new AppException(ErrorCode.POST_NOT_FOUND))
                .when(blockedContent).requireVisible("viewer", "blocked-author");

        assertThrows(AppException.class, () -> service.createComment(authentication, "post-1", null));

        verify(moderation, never()).moderate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(comments, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blockedCommentLikeRejectsBeforePersistenceOrCounterUpdate() {
        CommentRepository comments = mock(CommentRepository.class);
        CommentLikeRepository likes = mock(CommentLikeRepository.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
        BlockedContentPolicy blockedContent = mock(BlockedContentPolicy.class);
        CommentService service = new CommentService(
                comments,
                mock(PostRepository.class),
                likes,
                mock(ReplyRepository.class),
                mock(ReplyLikeRepository.class),
                kafka,
                mock(CommentMapper.class),
                mock(ProfileProvider.class),
                mock(ContentModerationProvider.class),
                mongoTemplate,
                blockedContent);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("viewer");
        Comment comment = mock(Comment.class);
        when(comment.getUserId()).thenReturn("blocked-author");
        when(comments.findById("comment-1")).thenReturn(Optional.of(comment));
        doThrow(new AppException(ErrorCode.POST_NOT_FOUND))
                .when(blockedContent).requireVisible("viewer", "blocked-author");

        assertThrows(AppException.class, () -> service.toggleLike(authentication, "comment-1"));

        verify(likes, never()).save(org.mockito.ArgumentMatchers.any());
        verify(likes, never()).deleteByCommentIdAndUserId(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        verify(mongoTemplate, never()).updateFirst(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(Comment.class));
        verify(kafka, never()).send(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
