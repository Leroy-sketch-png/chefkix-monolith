package com.chefkix.identity.repository;

import com.chefkix.identity.entity.ResetPasswordRequest;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResetPasswordRepository extends MongoRepository<ResetPasswordRequest, String> {

  Optional<ResetPasswordRequest> findByEmail(String email);
}
