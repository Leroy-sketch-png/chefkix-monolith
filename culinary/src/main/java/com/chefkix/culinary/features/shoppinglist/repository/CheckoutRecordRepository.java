package com.chefkix.culinary.features.shoppinglist.repository;

import com.chefkix.culinary.features.shoppinglist.entity.CheckoutRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CheckoutRecordRepository extends MongoRepository<CheckoutRecord, String> {

    Optional<CheckoutRecord> findByOrderId(String orderId);

    List<CheckoutRecord> findByUserIdOrderByCreatedAtDesc(String userId);

    long countByUserIdAndCreatedAtAfter(String userId, Instant after);

    long countByProviderAndCreatedAtAfter(String provider, Instant after);
}
