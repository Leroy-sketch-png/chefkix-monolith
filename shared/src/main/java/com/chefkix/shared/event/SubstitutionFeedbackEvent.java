package com.chefkix.shared.event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event emitted when a user interacts with a substitution during or after a cooking session.
 * Used by the HGAT Flywheel to update dynamic graph edge weights based on real cooking outcomes.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonTypeName("SUBSTITUTION_FEEDBACK_ACTION")
public class SubstitutionFeedbackEvent extends BaseEvent {

    private String sessionId;
    private String originalIngredient;
    private String substituteIngredient;
    private String technique;
    private String cuisine;
    private SubstitutionFeedbackChoice choice;
    private boolean accepted;
    private boolean sessionCompleted;
    private Double userRating; // 1.0 - 5.0 scale, nullable
    private String tasteFeedback;
    private boolean shared;

    @Builder
    public SubstitutionFeedbackEvent(String userId, String sessionId, String originalIngredient,
                                     String substituteIngredient, String technique, String cuisine,
                                     SubstitutionFeedbackChoice choice, boolean accepted,
                                     boolean sessionCompleted, Double userRating, String tasteFeedback,
                                     boolean shared) {
        super("SUBSTITUTION_FEEDBACK_ACTION", userId);
        this.sessionId = sessionId;
        this.originalIngredient = originalIngredient;
        this.substituteIngredient = substituteIngredient;
        this.technique = technique;
        this.cuisine = cuisine;
        this.choice = choice;
        this.accepted = accepted;
        this.sessionCompleted = sessionCompleted;
        this.userRating = userRating;
        this.tasteFeedback = tasteFeedback;
        this.shared = shared;
        this.eventId = buildDeterministicEventId(userId, sessionId, originalIngredient, substituteIngredient);
    }

    private static String buildDeterministicEventId(String userId, String sessionId,
                                                     String orig, String sub) {
        String sId = sessionId != null ? sessionId : "anon_session";
        String uId = userId != null ? userId : "anon_user";
        String o = orig != null ? orig : "none";
        String s = sub != null ? sub : "none";
        return "sub_feedback:" + uId + ":" + sId + ":" + o + "->" + s;
    }
}
