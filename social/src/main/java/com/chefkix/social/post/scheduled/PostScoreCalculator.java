package com.chefkix.social.post.scheduled;

import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled; // Required
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostScoreCalculator {

    private final PostRepository postRepository;
    private final MongoTemplate mongoTemplate; // Used for efficient updates
    private static final double GRAVITY = 2.0;

    /**
     * This task runs automatically every 10 minutes.
     * (fixedDelay = 600000 milliseconds)
     */
    @Scheduled(fixedDelay = 600000)
    public void updateTrendingScores() {
        try {
            log.info("Starting hot score update task...");

            // 1. Only calculate for posts in the last 7 days
            Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
            List<Post> recentPosts = postRepository.findByCreatedAtAfter(sevenDaysAgo);

            Instant now = Instant.now();

            // 2. Iterate, calculate and update
            for (Post post : recentPosts) {
                double newHotScore = calculateHotScore(post, now);

                // 3. Update new 'hotScore' in DB
                Query query = Query.query(Criteria.where("id").is(post.getId()));
                Update update = new Update().set("hotScore", newHotScore);
                mongoTemplate.updateFirst(query, update, Post.class);
            }
            log.info("Completed hot score update for {} posts.", recentPosts.size());
        } catch (Exception e) {
            log.error("Error updating hot scores. Task will retry next cycle.", e);
        }
    }

    /**
     * Score calculation function
     */
    private double calculateHotScore(Post post, Instant now) {
        int likes = (post.getLikes() != null) ? post.getLikes() : 0;
        int comments = (post.getCommentCount() != null) ? post.getCommentCount() : 0;
        double scoreBasis = (double) (likes + (comments * 2));

        if (scoreBasis == 0) return 0.0;

        long hoursSincePost = Duration.between(post.getCreatedAt(), now).toHours() + 1;
        return scoreBasis / Math.pow(hoursSincePost + GRAVITY, 1.8);
    }
}