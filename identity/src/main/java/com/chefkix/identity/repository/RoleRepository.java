package com.chefkix.identity.repository;

import com.chefkix.identity.entity.Role;
import com.chefkix.identity.enums.RoleName;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends MongoRepository<Role, String> {
  Optional<Role> findByName(RoleName name);
}
