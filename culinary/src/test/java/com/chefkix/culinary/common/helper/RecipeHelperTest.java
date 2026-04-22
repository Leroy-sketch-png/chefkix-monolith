package com.chefkix.culinary.common.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import com.chefkix.social.api.PostProvider;
import com.chefkix.social.api.dto.PostLinkInfo;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.shared.event.XpRewardEvent;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class RecipeHelperTest {

  @Mock
  private RecipeRepository recipeRepository;

  @Mock
  private CookingSessionRepository sessionRepository;

  @Mock
  private PostProvider postProvider;

  @Mock
  private KafkaTemplate<String, Object> kafkaTemplate;

  @Mock
  private MongoTemplate mongoTemplate;

  private RecipeHelper helper;

  @BeforeEach
  void setUp() {
    helper = new RecipeHelper(recipeRepository, sessionRepository, postProvider, kafkaTemplate, mongoTemplate);
  }

  @Test
  void calculateFinalXpForLinkingAppliesHalfDecayAfterSevenDays() {
    CookingSession session = CookingSession.builder()
        .pendingXp(80.0)
        .completedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(8))
        .build();

    int finalXp = helper.calculateFinalXpForLinking(
        session,
        PostLinkInfo.builder().photoCount(2).build());

    assertThat(finalXp).isEqualTo(40);
  }

  @Test
  void calculateFinalXpForLinkingReturnsZeroAfterFourteenDays() {
    CookingSession session = CookingSession.builder()
        .pendingXp(80.0)
        .completedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(15))
        .build();

    int finalXp = helper.calculateFinalXpForLinking(
        session,
        PostLinkInfo.builder().photoCount(2).build());

    assertThat(finalXp).isZero();
  }

  @Test
  void validateSessionForLinkingRejectsExpiredSession() {
    CookingSession session = CookingSession.builder()
        .id("session-1")
        .userId("user-1")
        .status(SessionStatus.EXPIRED)
        .build();

    when(sessionRepository.findById("session-1")).thenReturn(Optional.of(session));

    AppException thrown = assertThrows(
        AppException.class,
        () -> helper.validateSessionForLinking("session-1", "user-1"));

    assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.INVALID_ACTION);
    assertThat(thrown.getMessage()).contains("Session must be COMPLETED");
  }

  @Test
  void validateSessionForLinkingRejectsPostDeletedSessionAsAlreadyLinked() {
    CookingSession session = CookingSession.builder()
        .id("session-1")
        .userId("user-1")
        .status(SessionStatus.POST_DELETED)
        .build();

    when(sessionRepository.findById("session-1")).thenReturn(Optional.of(session));

    AppException thrown = assertThrows(
        AppException.class,
        () -> helper.validateSessionForLinking("session-1", "user-1"));

    assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.SESSION_ALREADY_LINKED);
  }

  @Test
  void sendXpEventWithBadgesPublishesRecipeContextForLinkingPost() {
    helper.sendXpEventWithBadges(
        "user-1",
        70.0,
        "LINKING_POST",
        "session-1",
        "Linking Post ID: post-1",
        java.util.List.of("broth-master"),
        "recipe-1");

    ArgumentCaptor<XpRewardEvent> eventCaptor = ArgumentCaptor.forClass(XpRewardEvent.class);
    verify(kafkaTemplate).send(eq("xp-delivery"), eventCaptor.capture());
    XpRewardEvent event = eventCaptor.getValue();
    assertThat(event.getUserId()).isEqualTo("user-1");
    assertThat(event.getAmount()).isEqualTo(70.0);
    assertThat(event.getSource()).isEqualTo("LINKING_POST");
    assertThat(event.getSessionId()).isEqualTo("session-1");
    assertThat(event.getRecipeId()).isEqualTo("recipe-1");
    assertThat(event.getBadges()).containsExactly("broth-master");
  }

  @Test
  void processCreatorBonusSkipsWhenCookerOwnsRecipe() {
    Recipe recipe = Recipe.builder()
        .id("recipe-1")
        .userId("creator-1")
        .xpReward(125)
        .title("Spicy Noodles")
        .build();

    boolean awarded = helper.processCreatorBonus(recipe, "creator-1", "session-1");

    assertThat(awarded).isFalse();
    verify(kafkaTemplate, never()).send(eq("xp-delivery"), org.mockito.ArgumentMatchers.any(XpRewardEvent.class));
  }

  @Test
  void processCreatorBonusPublishesEventForOtherCooker() {
    Recipe recipe = Recipe.builder()
        .id("recipe-1")
        .userId("creator-1")
        .xpReward(125)
        .title("Spicy Noodles")
        .build();

    boolean awarded = helper.processCreatorBonus(recipe, "cooker-1", "session-1");

    assertThat(awarded).isTrue();
    ArgumentCaptor<XpRewardEvent> eventCaptor = ArgumentCaptor.forClass(XpRewardEvent.class);
    verify(kafkaTemplate).send(eq("xp-delivery"), eventCaptor.capture());
    XpRewardEvent event = eventCaptor.getValue();
    assertThat(event.getUserId()).isEqualTo("creator-1");
    assertThat(event.getAmount()).isEqualTo(5.0);
    assertThat(event.getSource()).isEqualTo("CREATOR_BONUS");
    assertThat(event.getRecipeId()).isEqualTo("recipe-1");
    assertThat(event.getSessionId()).isEqualTo("session-1");
  }
}