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
import com.chefkix.social.post.dto.request.ReplyRequest;
import com.chefkix.social.post.entity.Comment;
import com.chefkix.social.post.repository.CommentRepository;
import com.chefkix.social.post.repository.ReplyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ReplyServiceModerationTest {

    @Mock private CommentRepository commentRepository;
    @Mock private ReplyRepository replyRepository;
    @Mock private ProfileProvider profileProvider;
    @Mock private ContentModerationProvider contentModerationProvider;

    @InjectMocks private ReplyService replyService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", "password"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void flaggedReplyIsRejectedBeforePersistence() {
        String content = "ambiguous cooking reply";
        when(profileProvider.getBasicProfile("user-1")).thenReturn(BasicProfileInfo.builder()
                .userId("user-1")
                .displayName("Test Chef")
                .build());
        when(commentRepository.findById("comment-1"))
                .thenReturn(java.util.Optional.of(Comment.builder()
                        .id("comment-1")
                        .userId("other-user")
                        .displayName("Other Chef")
                        .build()));
        when(contentModerationProvider.moderate(content, "comment"))
                .thenReturn(new ContentModerationProvider.ModerationResult(
                        "flag", "off_topic", "medium", 0.5, "Manual review required"));

        assertThatThrownBy(() -> replyService.createReply(ReplyRequest.builder()
                .parentCommentId("comment-1")
                .content(content)
                .build()))
                .isInstanceOf(AppException.class);

        verify(replyRepository, never()).save(any());
    }
}
