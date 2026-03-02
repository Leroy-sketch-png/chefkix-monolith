package com.chefkix.culinary.features.interaction.repository;

import com.chefkix.culinary.features.interaction.entity.RecipeLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RecipeLikeRepository extends MongoRepository<RecipeLike, String> {
    boolean existsByRecipeIdAndUserId(String recipeId, String userId);
    Optional<RecipeLike> findByRecipeIdAndUserId(String recipeId, String userId);
    List<RecipeLike> findAllByCreatedAtAfter(Instant date);
    
    // Get all recipes liked by a user (for /liked endpoint)
    List<RecipeLike> findAllByUserId(String userId);
    Page<RecipeLike> findAllByUserId(String userId, Pageable pageable);
    
    // Count likes for a user
    long countByUserId(String userId);
}