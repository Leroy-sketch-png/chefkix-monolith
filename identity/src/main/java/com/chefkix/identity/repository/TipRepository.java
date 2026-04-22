package com.chefkix.identity.repository;

import com.chefkix.identity.entity.Tip;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface TipRepository extends MongoRepository<Tip, String> {

    List<Tip> findByCreatorIdOrderByCreatedAtDesc(String creatorId);

    List<Tip> findByTipperIdOrderByCreatedAtDesc(String tipperId);

    long countByCreatorId(String creatorId);

    long countByCreatorIdAndCreatedAtAfter(String creatorId, Instant after);

    void deleteAllByTipperId(String tipperId);

    void deleteAllByCreatorId(String creatorId);
}
