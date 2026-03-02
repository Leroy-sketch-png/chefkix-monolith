package com.chefkix.culinary.features.recipe.repository;

import com.chefkix.culinary.features.recipe.entity.RecipeCompletion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;

public interface CompletionRepository extends MongoRepository<RecipeCompletion, String> {
    int countByUserIdAndCompletedAtAfter(String userId, LocalDateTime localDateTime);
}
