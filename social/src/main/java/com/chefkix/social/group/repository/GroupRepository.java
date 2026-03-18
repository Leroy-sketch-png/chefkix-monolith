package com.chefkix.social.group.repository;

import com.chefkix.social.chat.entity.Conversation;
import com.chefkix.social.group.dto.query.GroupExploreQuery;
import com.chefkix.social.group.entity.Group;
import com.chefkix.social.group.repository.custom.GroupCustomRepository;
import io.micrometer.observation.ObservationFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository extends MongoRepository<Group, String>, GroupCustomRepository {

    Page<Group> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description, Pageable pageable);
}