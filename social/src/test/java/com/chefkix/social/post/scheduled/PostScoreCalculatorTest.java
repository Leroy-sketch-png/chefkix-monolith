package com.chefkix.social.post.scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.policy.TrendingPolicy;
import com.chefkix.social.post.repository.PostRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

@ExtendWith(MockitoExtension.class)
class PostScoreCalculatorTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private PostScoreCalculator calculator;

    @Test
    void updateTrendingScoresUsesTheSharedEligibilityWindow() {
        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        when(postRepository.findByCreatedAtAfter(cutoff.capture())).thenReturn(List.<Post>of());
        Instant before = Instant.now();

        calculator.updateTrendingScores();
        Instant after = Instant.now();

        verify(postRepository).findByCreatedAtAfter(cutoff.getValue());
        verifyNoInteractions(mongoTemplate);
        assertThat(cutoff.getValue())
                .isBetween(
                        TrendingPolicy.cutoff(before),
                        TrendingPolicy.cutoff(after));
    }
}
