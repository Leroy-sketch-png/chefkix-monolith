package com.chefkix.culinary.features.duel.service;

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
import com.chefkix.shared.event.DuelEvent;
import com.chefkix.shared.event.XpRewardEvent;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuelService {

    private static final String REMINDER_TOPIC = "reminder-delivery";
    private static final Duration ACCEPT_WINDOW = Duration.ofHours(48);
    private static final Duration COOK_WINDOW = Duration.ofHours(24);
    private static final int DEFAULT_BONUS_XP = 50;

    private final CookingDuelRepository duelRepository;
    private final RecipeRepository recipeRepository;
    private final ProfileProvider profileProvider;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    public DuelResponse createDuel(String challengerId, CreateDuelRequest request) {
        if (challengerId.equals(request.getOpponentId())) {
            throw new AppException(ErrorCode.DUEL_CANNOT_CHALLENGE_SELF);
        }

        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        Instant now = Instant.now();
        duelRepository.findActiveBetween(challengerId, request.getOpponentId(), request.getRecipeId())
                .filter(duel -> !expireIfOverdue(duel, now))
                .ifPresent(duel -> { throw new AppException(ErrorCode.DUEL_ALREADY_EXISTS); });

        if (profileProvider.isBlocked(challengerId, request.getOpponentId())) {
            throw new AppException(ErrorCode.DUEL_NOT_PARTICIPANT);
        }

        CookingDuel duel = CookingDuel.builder()
                .challengerId(challengerId)
                .opponentId(request.getOpponentId())
                .recipeId(recipe.getId())
                .recipeTitle(recipe.getTitle())
                .recipeCoverUrl(recipe.getCoverImageUrl() != null && !recipe.getCoverImageUrl().isEmpty()
                        ? recipe.getCoverImageUrl().get(0) : null)
                .status(DuelStatus.PENDING)
                .message(request.getMessage())
                .bonusXp(DEFAULT_BONUS_XP)
                .acceptDeadline(now.plus(ACCEPT_WINDOW))
                .build();

        duel = duelRepository.save(duel);

        sendDuelNotification(duel, "INVITE", request.getOpponentId());

        return toResponse(duel);
    }


    @Transactional(noRollbackFor = AppException.class)
    public DuelResponse acceptDuel(String userId, String duelId) {
        CookingDuel duel = getDuelOrThrow(duelId);
        validateOpponent(duel, userId);
        expireIfOverdue(duel, Instant.now());
        validateStatus(duel, DuelStatus.PENDING);

        duel.setStatus(DuelStatus.ACCEPTED);
        duel.setAcceptedAt(Instant.now());
        duel.setCookDeadline(Instant.now().plus(COOK_WINDOW));
        duel = duelRepository.save(duel);

        sendDuelNotification(duel, "ACCEPTED", duel.getChallengerId());

        return toResponse(duel);
    }

    @Transactional(noRollbackFor = AppException.class)
    public DuelResponse declineDuel(String userId, String duelId) {
        CookingDuel duel = getDuelOrThrow(duelId);
        validateOpponent(duel, userId);
        expireIfOverdue(duel, Instant.now());
        validateStatus(duel, DuelStatus.PENDING);

        duel.setStatus(DuelStatus.DECLINED);
        duel = duelRepository.save(duel);

        sendDuelNotification(duel, "DECLINED", duel.getChallengerId());

        return toResponse(duel);
    }

    @Transactional(noRollbackFor = AppException.class)
    public DuelResponse cancelDuel(String userId, String duelId) {
        CookingDuel duel = getDuelOrThrow(duelId);
        if (!duel.getChallengerId().equals(userId)) {
            throw new AppException(ErrorCode.DUEL_NOT_PARTICIPANT);
        }
        expireIfOverdue(duel, Instant.now());
        validateStatus(duel, DuelStatus.PENDING);

        duel.setStatus(DuelStatus.CANCELLED);
        duel = duelRepository.save(duel);

        return toResponse(duel);
    }


    /**
     */
    @Transactional
    public void onSessionCompleted(String userId, CookingSession session) {
        List<CookingDuel> duels = duelRepository.findByParticipantAndStatusIn(
                userId, List.of(DuelStatus.ACCEPTED, DuelStatus.IN_PROGRESS));
        Instant now = Instant.now();

        for (CookingDuel duel : duels) {
            if (expireIfOverdue(duel, now)) continue;
            if (!duel.getRecipeId().equals(session.getRecipeId())) continue;

            boolean isChallenger = duel.getChallengerId().equals(userId);
            boolean isOpponent = duel.getOpponentId().equals(userId);

            if (isChallenger && duel.getChallengerSessionId() == null) {
                duel.setChallengerSessionId(session.getId());
                duel.setChallengerScore(computeScore(session));
                duel.setStatus(DuelStatus.IN_PROGRESS);
                duel = duelRepository.save(duel);
                checkIfBothDone(duel);
                break;
            } else if (isOpponent && duel.getOpponentSessionId() == null) {
                duel.setOpponentSessionId(session.getId());
                duel.setOpponentScore(computeScore(session));
                duel.setStatus(DuelStatus.IN_PROGRESS);
                duel = duelRepository.save(duel);
                checkIfBothDone(duel);
                break;
            }
        }
    }


    public DuelResponse getDuel(String userId, String duelId) {
        CookingDuel duel = getDuelOrThrow(duelId);
        validateParticipant(duel, userId);
        expireIfOverdue(duel, Instant.now());
        return toResponse(duel);
    }

    public List<DuelResponse> getMyDuels(String userId) {
        List<CookingDuel> duels = duelRepository.findByParticipant(userId);
        reconcileOverdue(duels, Instant.now());
        return duels.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<DuelResponse> getMyActiveDuels(String userId) {
        List<CookingDuel> duels = duelRepository.findByParticipantAndStatusIn(
                userId, List.of(DuelStatus.PENDING, DuelStatus.ACCEPTED, DuelStatus.IN_PROGRESS));
        Instant now = Instant.now();
        reconcileOverdue(duels, now);
        return duels.stream()
                .filter(duel -> duel.getStatus() != DuelStatus.EXPIRED)
                .map(this::toResponse)
                .toList();
    }

    public List<DuelResponse> getPendingInvites(String userId) {
        List<CookingDuel> duels = duelRepository.findByOpponentIdAndStatus(userId, DuelStatus.PENDING);
        Instant now = Instant.now();
        reconcileOverdue(duels, now);
        return duels.stream()
                .filter(duel -> duel.getStatus() != DuelStatus.EXPIRED)
                .map(this::toResponse)
                .toList();
    }


    private int computeScore(CookingSession session) {
        int score = 0;
        Recipe recipe = recipeRepository.findById(session.getRecipeId()).orElse(null);
        int totalSteps = recipe != null && recipe.getSteps() != null ? recipe.getSteps().size() : 0;
        int estimatedTimeMinutes = recipe != null ? recipe.getTotalTimeMinutes() : 0;

        if (totalSteps > 0 && session.getCompletedSteps() != null) {
            double ratio = (double) session.getCompletedSteps().size() / totalSteps;
            score += (int) (ratio * 60);
        }

        if (estimatedTimeMinutes > 0 && session.getStartedAt() != null && session.getCompletedAt() != null) {
            long actualMinutes = Duration.between(session.getStartedAt(), session.getCompletedAt()).toMinutes();
            double timeRatio = (double) estimatedTimeMinutes / Math.max(1, actualMinutes);
            int timeScore = (int) Math.min(25, timeRatio * 25);
            score += timeScore;
        }

        if ("COMPLETED".equals(session.getStatus().name())) {
            score += 15;
        }

        return Math.min(100, score);
    }

    private void checkIfBothDone(CookingDuel duel) {
        if (duel.getChallengerSessionId() != null && duel.getOpponentSessionId() != null) {
            completeDuel(duel);
        }
    }

    private void completeDuel(CookingDuel duel) {
        int cScore = duel.getChallengerScore() != null ? duel.getChallengerScore() : 0;
        int oScore = duel.getOpponentScore() != null ? duel.getOpponentScore() : 0;

        if (cScore > oScore) {
            duel.setWinnerId(duel.getChallengerId());
        } else if (oScore > cScore) {
            duel.setWinnerId(duel.getOpponentId());
        }

        duel.setStatus(DuelStatus.COMPLETED);
        duel.setCompletedAt(Instant.now());
        duelRepository.save(duel);

        if (duel.getWinnerId() != null) {
            XpRewardEvent xpEvent = XpRewardEvent.builder()
                    .userId(duel.getWinnerId())
                    .amount(duel.getBonusXp())
                    .source("DUEL_WIN")
                    .description("Duel victory bonus")
                    .build();
            kafkaTemplate.send("xp-delivery", xpEvent);
            log.info("Awarded {} bonus XP to duel winner {}", duel.getBonusXp(), duel.getWinnerId());
        }

        sendDuelNotification(duel, "COMPLETED", duel.getChallengerId());
        sendDuelNotification(duel, "COMPLETED", duel.getOpponentId());

        log.info("Duel {} completed. Winner: {} ({}:{} vs {}:{})",
                duel.getId(), duel.getWinnerId(),
                duel.getChallengerId(), cScore,
                duel.getOpponentId(), oScore);
    }


    private CookingDuel getDuelOrThrow(String duelId) {
        return duelRepository.findById(duelId)
                .orElseThrow(() -> new AppException(ErrorCode.DUEL_NOT_FOUND));
    }

    private void validateOpponent(CookingDuel duel, String userId) {
        if (!duel.getOpponentId().equals(userId)) {
            throw new AppException(ErrorCode.DUEL_NOT_PARTICIPANT);
        }
    }

    private void validateParticipant(CookingDuel duel, String userId) {
        if (!duel.getChallengerId().equals(userId) && !duel.getOpponentId().equals(userId)) {
            throw new AppException(ErrorCode.DUEL_NOT_PARTICIPANT);
        }
    }

    private void validateStatus(CookingDuel duel, DuelStatus expected) {
        if (duel.getStatus() != expected) {
            throw new AppException(ErrorCode.DUEL_NOT_PENDING);
        }
    }

    private void reconcileOverdue(List<CookingDuel> duels, Instant now) {
        duels.forEach(duel -> expireIfOverdue(duel, now));
    }

    private boolean expireIfOverdue(CookingDuel duel, Instant now) {
        Instant deadline = switch (duel.getStatus()) {
            case PENDING -> duel.getAcceptDeadline();
            case ACCEPTED, IN_PROGRESS -> duel.getCookDeadline();
            default -> null;
        };

        if (deadline == null || deadline.isAfter(now)) {
            return false;
        }

        duel.setStatus(DuelStatus.EXPIRED);
        duelRepository.save(duel);
        return true;
    }

    private void sendDuelNotification(CookingDuel duel, String action, String targetUserId) {
        try {
            BasicProfileInfo challenger = profileProvider.getBasicProfile(duel.getChallengerId());
            BasicProfileInfo opponent = profileProvider.getBasicProfile(duel.getOpponentId());

            DuelEvent event = DuelEvent.builder()
                    .userId(targetUserId)
                    .duelId(duel.getId())
                    .duelAction(action)
                    .challengerName(challenger.getDisplayName() != null ? challenger.getDisplayName() : challenger.getUsername())
                    .opponentName(opponent.getDisplayName() != null ? opponent.getDisplayName() : opponent.getUsername())
                    .recipeTitle(duel.getRecipeTitle())
                    .recipeCoverUrl(duel.getRecipeCoverUrl())
                    .message(duel.getMessage())
                    .winnerId(duel.getWinnerId())
                    .bonusXp(duel.getBonusXp())
                    .build();

            kafkaTemplate.send(REMINDER_TOPIC, event);
        } catch (Exception e) {
            log.warn("Failed to send duel notification for duel {}: {}", duel.getId(), e.getMessage());
        }
    }

    private DuelResponse toResponse(CookingDuel duel) {
        BasicProfileInfo challengerProfile = null;
        BasicProfileInfo opponentProfile = null;
        try {
            challengerProfile = profileProvider.getBasicProfile(duel.getChallengerId());
        } catch (Exception e) {
            log.warn("Failed to get challenger profile: {}", e.getMessage());
        }
        try {
            opponentProfile = profileProvider.getBasicProfile(duel.getOpponentId());
        } catch (Exception e) {
            log.warn("Failed to get opponent profile: {}", e.getMessage());
        }

        return DuelResponse.builder()
                .id(duel.getId())
                .challengerId(duel.getChallengerId())
                .challengerName(challengerProfile != null
                        ? (challengerProfile.getDisplayName() != null ? challengerProfile.getDisplayName() : challengerProfile.getUsername())
                        : "Unknown")
                .challengerAvatar(challengerProfile != null ? challengerProfile.getAvatarUrl() : null)
                .opponentId(duel.getOpponentId())
                .opponentName(opponentProfile != null
                        ? (opponentProfile.getDisplayName() != null ? opponentProfile.getDisplayName() : opponentProfile.getUsername())
                        : "Unknown")
                .opponentAvatar(opponentProfile != null ? opponentProfile.getAvatarUrl() : null)
                .recipeId(duel.getRecipeId())
                .recipeTitle(duel.getRecipeTitle())
                .recipeCoverUrl(duel.getRecipeCoverUrl())
                .status(duel.getStatus())
                .message(duel.getMessage())
                .challengerScore(duel.getChallengerScore())
                .opponentScore(duel.getOpponentScore())
                .winnerId(duel.getWinnerId())
                .bonusXp(duel.getBonusXp())
                .challengerSessionId(duel.getChallengerSessionId())
                .opponentSessionId(duel.getOpponentSessionId())
                .acceptDeadline(duel.getAcceptDeadline())
                .cookDeadline(duel.getCookDeadline())
                .createdAt(duel.getCreatedAt())
                .acceptedAt(duel.getAcceptedAt())
                .completedAt(duel.getCompletedAt())
                .build();
    }
}
