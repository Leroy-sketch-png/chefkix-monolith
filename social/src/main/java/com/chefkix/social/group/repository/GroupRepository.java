package com.chefkix.social.group.repository;

import com.chefkix.social.group.entity.Group;
import com.chefkix.social.group.repository.custom.GroupCustomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends MongoRepository<Group, String>, GroupCustomRepository {

    List<Group> findAllByOwnerId(String ownerId);

    List<Group> findAllByCreatorId(String creatorId);

    Page<Group> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description, Pageable pageable);
}