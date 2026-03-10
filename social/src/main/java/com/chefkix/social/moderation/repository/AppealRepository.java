package com.chefkix.social.moderation.repository;

import com.chefkix.social.moderation.entity.Appeal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppealRepository extends MongoRepository<Appeal, String> {

    Optional<Appeal> findByBanIdAndStatus(String banId, String status);

    List<Appeal> findByUserId(String userId);

    List<Appeal> findByStatus(String status);
}
