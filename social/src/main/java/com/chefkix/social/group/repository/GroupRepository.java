package com.chefkix.social.group.repository;

import com.chefkix.social.chat.entity.Conversation;
import com.chefkix.social.group.entity.Group;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository extends MongoRepository<Group, String> {


}