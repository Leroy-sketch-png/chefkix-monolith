package com.chefkix.identity.migration;

import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignupCredentialSecurityMigration implements ApplicationRunner {

  private static final String COLLECTION = "signup_requests";

  private final MongoTemplate mongoTemplate;

  @Override
  public void run(ApplicationArguments args) {
    UpdateResult result =
        mongoTemplate.updateMulti(
            Query.query(Criteria.where("password").exists(true)),
            new Update().unset("password"),
            COLLECTION);

    if (result.getModifiedCount() > 0) {
      log.warn(
          "Removed legacy plaintext credentials from {} pending signup request(s)",
          result.getModifiedCount());
    }
  }
}
