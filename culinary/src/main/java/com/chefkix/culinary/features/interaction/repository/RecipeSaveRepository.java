package com.chefkix.culinary.features.interaction.repository;

import com.chefkix.culinary.features.interaction.entity.RecipeSave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeSaveRepository extends MongoRepository<RecipeSave, String> {
    boolean existsByRecipeIdAndUserId(String recipeId, String userId);
    Optional<RecipeSave> findByRecipeIdAndUserId(String recipeId, String userId);
    
    // Get all recipes saved by a user (for /saved endpoint)
    List<RecipeSave> findAllByUserId(String userId);
    Page<RecipeSave> findAllByUserId(String userId, Pageable pageable);
    
    // Count saves for a user
    long countByUserId(String userId);
}