package com.chefkix.culinary.features.report.repository;


import com.chefkix.culinary.features.report.entity.Appeal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppealRepository extends MongoRepository<Appeal, String> {
    // Tìm kháng cáo theo completionId
    Optional<Appeal> findByCompletionId(String completionId);
}