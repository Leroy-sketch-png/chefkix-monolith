package com.chefkix.culinary.features.pantry.repository;

import com.chefkix.culinary.features.pantry.entity.PantryItem;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PantryItemRepository extends MongoRepository<PantryItem, String> {

    List<PantryItem> findByUserId(String userId, Sort sort);

    List<PantryItem> findByUserIdAndCategory(String userId, String category, Sort sort);

    Optional<PantryItem> findByUserIdAndNormalizedName(String userId, String normalizedName);

    Optional<PantryItem> findByIdAndUserId(String id, String userId);

    void deleteAllByUserId(String userId);

    List<PantryItem> findByUserIdAndExpiryDateBefore(String userId, LocalDate date);

    List<PantryItem> findByUserIdAndExpiryDateBetween(String userId, LocalDate from, LocalDate to);

    void deleteByIdAndUserId(String id, String userId);

    void deleteByUserIdAndExpiryDateBefore(String userId, LocalDate date);

    long countByUserId(String userId);

    long countByUserIdAndExpiryDateBetween(String userId, LocalDate from, LocalDate to);
}
