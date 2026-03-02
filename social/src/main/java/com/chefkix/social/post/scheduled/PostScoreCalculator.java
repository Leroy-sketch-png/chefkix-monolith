package com.chefkix.social.post.scheduled;

import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled; // Quan trọng
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
    private final MongoTemplate mongoTemplate; // Dùng để update hiệu quả
    private static final double GRAVITY = 2.0;

    /**
     * Tác vụ này tự động chạy 10 phút một lần.
     * (fixedDelay = 600000 milliseconds)
     */
    @Scheduled(fixedDelay = 600000)
    public void updateTrendingScores() {
        log.info("Bắt đầu chạy tác vụ cập nhật điểm hot...");

        // 1. Chỉ tính cho các post trong 7 ngày qua
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        List<Post> recentPosts = postRepository.findByCreatedAtAfter(sevenDaysAgo);

        Instant now = Instant.now();

        // 2. Lặp qua, tính toán và cập nhật
        for (Post post : recentPosts) {
            double newHotScore = calculateHotScore(post, now);

            // 3. Cập nhật 'hotScore' mới vào DB
            Query query = Query.query(Criteria.where("id").is(post.getId()));
            Update update = new Update().set("hotScore", newHotScore);
            mongoTemplate.updateFirst(query, update, Post.class);
        }
        log.info("Hoàn thành cập nhật điểm hot cho {} bài post.", recentPosts.size());
    }

    /**
     * Hàm tính điểm (giống hệt hàm cũ của bạn)
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