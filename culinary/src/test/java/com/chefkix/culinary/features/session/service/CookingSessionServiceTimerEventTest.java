package com.chefkix.culinary.features.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.common.enums.TimerEventType;
import com.chefkix.culinary.common.helper.RecipeHelper;
import com.chefkix.culinary.features.challenge.service.ChallengeService;
import com.chefkix.culinary.features.duel.service.DuelService;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.entity.Step;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.features.room.repository.CookingRoomRedisRepository;
import com.chefkix.culinary.features.session.dto.request.TimerEventRequest;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.session.mapper.CookingSessionMapper;
import com.chefkix.culinary.features.session.repository.ActiveCookingRedisRepository;
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.social.api.PostProvider;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class CookingSessionServiceTimerEventTest {

  @Mock
  private CookingSessionRepository sessionRepository;

  @Mock
  private RecipeRepository recipeRepository;

  @Mock
  private CookingSessionMapper sessionMapper;

  @Mock
  private ChallengeService challengeService;

  @Mock
  private ProfileProvider profileProvider;

  @Mock
  private PostProvider postProvider;

  @Mock
  private CookingRoomRedisRepository roomRepository;

  @Mock
  private ActiveCookingRedisRepository activeCookingRepository;

  @Mock
  private com.chefkix.culinary.features.achievement.service.AchievementService achievementService;

  @Mock
  private DuelService duelService;

  @Mock
  private KafkaTemplate<String, Object> kafkaTemplate;

  @Mock
  private MongoTemplate mongoTemplate;

  private CookingSessionService cookingSessionService;

  @BeforeEach
  void setUp() {
    RecipeHelper helper = new RecipeHelper(
        recipeRepository,
        sessionRepository,
        postProvider,
        kafkaTemplate,
        mongoTemplate);

    cookingSessionService = new CookingSessionService(
        sessionRepository,
        recipeRepository,
        sessionMapper,
        challengeService,
        helper,
        profileProvider,
        postProvider,
        roomRepository,
        activeCookingRepository,
        achievementService,
        duelService);
  }

  @Test
  void logTimerEventStartInitializesLegacyActiveTimers() {
    String userId = "user-1";
    String sessionId = "session-1";
    String recipeId = "recipe-1";
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

    CookingSession session = CookingSession.builder()
        .id(sessionId)
        .userId(userId)
        .recipeId(recipeId)
        .status(SessionStatus.IN_PROGRESS)
        .activeTimers(null)
        .timerEvents(null)
        .build();

    when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
    when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipeWithSteps(recipeId, 3, 2, 900)));

    TimerEventRequest request = new TimerEventRequest();
    request.setStepNumber(2);
    request.setEvent(TimerEventType.START);
    request.setClientTimestamp(now.minusSeconds(3));

    cookingSessionService.logTimerEvent(userId, sessionId, request);

    assertThat(session.getTimerEvents()).hasSize(1);
    assertThat(session.getActiveTimers()).hasSize(1);
    CookingSession.ActiveTimer activeTimer = session.getActiveTimers().get(0);
    assertThat(activeTimer.getStepNumber()).isEqualTo(2);
    assertThat(activeTimer.getTotalSeconds()).isEqualTo(900);
    assertThat(activeTimer.getRemainingSeconds()).isEqualTo(900);
    verify(sessionRepository).save(session);
  }

  @Test
  void logTimerEventCompleteInitializesCompletedStepsAndAdvancesStep() {
    String userId = "user-1";
    String sessionId = "session-1";
    String recipeId = "recipe-1";
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

    CookingSession.ActiveTimer activeTimer = CookingSession.ActiveTimer.builder()
        .stepNumber(2)
        .totalSeconds(900)
        .remainingSeconds(120)
        .startedAt(now.minusMinutes(13))
        .build();

    CookingSession session = CookingSession.builder()
        .id(sessionId)
        .userId(userId)
        .recipeId(recipeId)
        .status(SessionStatus.IN_PROGRESS)
        .currentStep(2)
        .activeTimers(new ArrayList<>(List.of(activeTimer)))
        .completedSteps(null)
        .timerEvents(new ArrayList<>())
        .build();

    when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
    when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipeWithSteps(recipeId, 3, 2, 900)));

    TimerEventRequest request = new TimerEventRequest();
    request.setStepNumber(2);
    request.setEvent(TimerEventType.COMPLETE);
    request.setClientTimestamp(now);

    cookingSessionService.logTimerEvent(userId, sessionId, request);

    assertThat(session.getTimerEvents()).hasSize(1);
    assertThat(session.getActiveTimers()).isEmpty();
    assertThat(session.getCompletedSteps()).containsExactly(2);
    assertThat(session.getCurrentStep()).isEqualTo(3);
    verify(sessionRepository).save(session);
  }

  private Recipe recipeWithSteps(String recipeId, int totalSteps, int timerStepNumber, int timerSeconds) {
    List<Step> steps = new ArrayList<>();
    for (int stepNumber = 1; stepNumber <= totalSteps; stepNumber++) {
      Step step = new Step();
      step.setStepNumber(stepNumber);
      step.setDescription("Step " + stepNumber);
      if (stepNumber == timerStepNumber) {
        step.setTimerSeconds(timerSeconds);
      }
      steps.add(step);
    }

    return Recipe.builder()
        .id(recipeId)
        .steps(steps)
        .build();
  }
}