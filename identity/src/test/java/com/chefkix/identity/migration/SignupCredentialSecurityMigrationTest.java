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
class SignupCredentialSecurityMigrationTest {

  @Mock private MongoTemplate mongoTemplate;

  @Test
  void removesLegacyPasswordFieldFromEveryPendingSignup() {
    when(mongoTemplate.updateMulti(
            org.mockito.ArgumentMatchers.any(Query.class),
            org.mockito.ArgumentMatchers.any(Update.class),
            eq("signup_requests")))
        .thenReturn(UpdateResult.acknowledged(2, 2L, null));
    SignupCredentialSecurityMigration migration =
        new SignupCredentialSecurityMigration(mongoTemplate);

    migration.run(new DefaultApplicationArguments(new String[0]));

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
    verify(mongoTemplate).updateMulti(query.capture(), update.capture(), eq("signup_requests"));
    assertThat(query.getValue().getQueryObject().toJson())
        .contains("password")
        .contains("$exists");
    assertThat(update.getValue().getUpdateObject().toJson())
        .contains("$unset")
        .contains("password");
  }
}
