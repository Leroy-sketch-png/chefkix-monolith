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

/** Initializes optimistic-lock versions for profiles created before @Version was introduced. */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserProfileVersionMigration implements ApplicationRunner {

  private static final String COLLECTION = "user_profiles";

  private final MongoTemplate mongoTemplate;

  @Override
  public void run(ApplicationArguments args) {
    UpdateResult result =
        mongoTemplate.updateMulti(
            Query.query(Criteria.where("version").is(null)),
            new Update().set("version", 0L),
            COLLECTION);

    if (result.getModifiedCount() > 0) {
      log.info(
          "Initialized optimistic-lock versions for {} legacy user profile(s)",
          result.getModifiedCount());
    }
  }
}
