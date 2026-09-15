package com.chefkix.culinary.features.session.service;

import com.chefkix.culinary.features.session.controller.SubstitutionFeedbackController.SubstitutionFeedbackRequest;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import com.chefkix.shared.event.SubstitutionFeedbackEvent;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubstitutionFeedbackService {

    static final String TOPIC = "substitution-feedback";
    private final CookingSessionRepository sessionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public String submit(
            String userId, String sessionId, SubstitutionFeedbackRequest request) {
        CookingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        if (!userId.equals(session.getUserId()) || session.isUserDeleted()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        SubstitutionFeedbackEvent event = SubstitutionFeedbackEvent.builder()
                .userId(userId)
                .sessionId(sessionId)
                .clientFeedbackId(request.getClientFeedbackId())
                .originalIngredient(request.getOriginalIngredient())
                .substituteIngredient(request.getSubstituteIngredient())
                .candidateReceipt(request.getCandidateReceipt())
                .technique(request.getTechnique())
                .cuisine(request.getCuisine())
                .accepted(request.isAccepted())
                .sessionCompleted(session.getStatus() != null && session.getStatus().countsAsCompletedCook())
                .userRating(request.getUserRating())
                .shared(session.getStatus() != null && session.getStatus().hasClaimedPostXp())
                .build();

        try {
            kafkaTemplate.send(TOPIC, userId, event).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to durably publish substitution feedback eventId={}", event.getEventId(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        log.info(
                "Published substitution feedback eventId={}, userId={}, sessionId={}",
                event.getEventId(), userId, sessionId);
        return event.getEventId();
    }
}
