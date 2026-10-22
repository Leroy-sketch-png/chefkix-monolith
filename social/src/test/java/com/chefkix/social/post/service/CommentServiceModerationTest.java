package com.chefkix.social.post.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.api.ContentModerationProvider;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.shared.exception.AppException;
import com.chefkix.social.post.dto.request.CommentRequest;
import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.repository.CommentRepository;
import com.chefkix.social.post.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class CommentServiceModerationTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private ProfileProvider profileProvider;
    @Mock private ContentModerationProvider contentModerationProvider;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks private CommentService commentService;

    @Test
    void flaggedCommentIsRejectedBeforePersistence() {
        String content = "ambiguous cooking comment";
        when(postRepository.findById("post-1")).thenReturn(java.util.Optional.of(Post.builder().id("post-1").build()));
        when(profileProvider.getBasicProfile("user-1")).thenReturn(BasicProfileInfo.builder()
                .userId("user-1")
                .displayName("Test Chef")
                .build());
        when(contentModerationProvider.moderate(content, "comment"))
                .thenReturn(new ContentModerationProvider.ModerationResult(
                        "flag", "off_topic", "medium", 0.5, "Manual review required"));

        assertThatThrownBy(() -> commentService.createComment(
                new TestingAuthenticationToken("user-1", null),
                "post-1",
                CommentRequest.builder().content(content).build()))
                .isInstanceOf(AppException.class);

        verify(commentRepository, never()).save(any());
        verify(mongoTemplate, never()).updateFirst(any(), any(), any(Class.class));
    }
}
