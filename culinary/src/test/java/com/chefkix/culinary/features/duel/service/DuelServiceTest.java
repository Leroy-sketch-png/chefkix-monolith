package com.chefkix.culinary.features.duel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.features.duel.dto.request.CreateDuelRequest;
import com.chefkix.culinary.features.duel.dto.response.DuelResponse;
import com.chefkix.culinary.features.duel.entity.CookingDuel;
import com.chefkix.culinary.features.duel.entity.DuelStatus;
import com.chefkix.culinary.features.duel.repository.CookingDuelRepository;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.shared.exception.AppException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class DuelServiceTest {

    @Mock private CookingDuelRepository duelRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private ProfileProvider profileProvider;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks private DuelService duelService;

    @Test
    void activeListPersistsAndOmitsOverdueDuels() {
        CookingDuel overdue = pending("overdue", Instant.now().minusSeconds(60));
        CookingDuel valid = pending("valid", Instant.now().plusSeconds(3600));
        when(duelRepository.findByParticipantAndStatusIn(anyString(), any()))
                .thenReturn(List.of(overdue, valid));

        List<DuelResponse> result = duelService.getMyActiveDuels("user-1");

        assertThat(result).extracting(DuelResponse::getId).containsExactly("valid");
        assertThat(overdue.getStatus()).isEqualTo(DuelStatus.EXPIRED);
        verify(duelRepository).save(overdue);
    }

    @Test
    void overdueInviteIsPersistedBeforeAcceptIsRejected() throws Exception {
        CookingDuel overdue = pending("overdue", Instant.now().minusSeconds(60));
        when(duelRepository.findById("overdue")).thenReturn(Optional.of(overdue));

        assertThrows(AppException.class, () -> duelService.acceptDuel("user-2", "overdue"));

        assertThat(overdue.getStatus()).isEqualTo(DuelStatus.EXPIRED);
        verify(duelRepository).save(overdue);

        Method accept = DuelService.class.getMethod("acceptDuel", String.class, String.class);
        assertThat(List.of(accept.getAnnotation(Transactional.class).noRollbackFor()))
                .contains(AppException.class);
    }

    @Test
    void overdueCookWindowCannotLinkACompletedSession() {
        CookingDuel overdue = CookingDuel.builder()
                .id("overdue")
                .challengerId("user-1")
                .opponentId("user-2")
                .recipeId("recipe-1")
                .status(DuelStatus.IN_PROGRESS)
                .cookDeadline(Instant.now().minusSeconds(60))
                .build();
        when(duelRepository.findByParticipantAndStatusIn(anyString(), any()))
                .thenReturn(List.of(overdue));

        duelService.onSessionCompleted("user-1", CookingSession.builder()
                .id("session-1")
                .recipeId("recipe-1")
                .status(SessionStatus.COMPLETED)
                .build());

        assertThat(overdue.getStatus()).isEqualTo(DuelStatus.EXPIRED);
        assertThat(overdue.getChallengerSessionId()).isNull();
        verify(duelRepository).save(overdue);
        verify(recipeRepository, never()).findById(anyString());
    }

    @Test
    void overdueDuplicateDoesNotBlockAReplacementDuel() {
        CookingDuel overdue = pending("overdue", Instant.now().minusSeconds(60));
        Recipe recipe = Recipe.builder().id("recipe-1").title("Noodles").build();
        CreateDuelRequest request = CreateDuelRequest.builder()
                .opponentId("user-2")
                .recipeId("recipe-1")
                .build();
        when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(recipe));
        when(duelRepository.findActiveBetween("user-1", "user-2", "recipe-1"))
                .thenReturn(Optional.of(overdue));
        when(profileProvider.isBlocked("user-1", "user-2")).thenReturn(false);
        when(profileProvider.getBasicProfile(anyString())).thenReturn(BasicProfileInfo.builder()
                .userId("profile")
                .username("chef")
                .build());
        when(duelRepository.save(any(CookingDuel.class))).thenAnswer(invocation -> {
            CookingDuel duel = invocation.getArgument(0);
            if (duel.getId() == null) duel.setId("replacement");
            return duel;
        });

        DuelResponse result = duelService.createDuel("user-1", request);

        assertThat(overdue.getStatus()).isEqualTo(DuelStatus.EXPIRED);
        assertThat(result.getId()).isEqualTo("replacement");
        assertThat(result.getStatus()).isEqualTo(DuelStatus.PENDING);
        verify(duelRepository, times(2)).save(any(CookingDuel.class));
    }

    private CookingDuel pending(String id, Instant deadline) {
        return CookingDuel.builder()
                .id(id)
                .challengerId("user-1")
                .opponentId("user-2")
                .recipeId("recipe-1")
                .recipeTitle("Noodles")
                .status(DuelStatus.PENDING)
                .acceptDeadline(deadline)
                .build();
    }
}
