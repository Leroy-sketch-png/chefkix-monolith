package com.chefkix.culinary.features.shoppinglist.repository;

import com.chefkix.culinary.features.shoppinglist.entity.ShoppingList;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ShoppingListRepository extends MongoRepository<ShoppingList, String> {
    List<ShoppingList> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<ShoppingList> findByIdAndUserId(String id, String userId);
    Optional<ShoppingList> findByShareToken(String shareToken);
    void deleteByIdAndUserId(String id, String userId);
    void deleteAllByUserId(String userId);
    long countByUserId(String userId);
}
