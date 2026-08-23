package com.chefkix.identity.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ExtendWith(MockitoExtension.class)
class UserProfileVersionMigrationTest {

  @Mock private MongoTemplate mongoTemplate;

  @Test
  void initializesOnlyProfilesWithoutAStoredVersion() {
    when(mongoTemplate.updateMulti(
            org.mockito.ArgumentMatchers.any(Query.class),
            org.mockito.ArgumentMatchers.any(Update.class),
            eq("user_profiles")))
        .thenReturn(UpdateResult.acknowledged(11, 11L, null));

    new UserProfileVersionMigration(mongoTemplate)
        .run(new DefaultApplicationArguments(new String[0]));

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
    verify(mongoTemplate).updateMulti(query.capture(), update.capture(), eq("user_profiles"));
    assertThat(query.getValue().getQueryObject().toJson())
        .contains("version")
        .contains("null");
    assertThat(update.getValue().getUpdateObject().toJson())
        .contains("$set")
        .contains("version")
        .contains("0");
  }
}
