package com.chefkix.culinary.features.session.entity;

import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.common.enums.TimerEventType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "cooking_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CookingSession {

    @Id
    String id;              // id: string

    String userId;          // userId: string
    String recipeId;        // recipeId: string
    String recipeTitle;
    List<String> coverImageUrl;
    SessionStatus status;   // status: enum

    String roomCode;        // Co-cooking room code (null for solo sessions)

    Integer currentStep;            // currentStep: number

    @Builder.Default
    List<Integer> completedSteps = new ArrayList<>(); // completedSteps: number[]

    // --- Timing ---
    LocalDateTime startedAt;    // startedAt: string (ISO8601)
    LocalDateTime pausedAt;     // pausedAt?: string
    LocalDateTime resumeDeadline;
    LocalDateTime completedAt;  // completedAt?: string
    LocalDateTime abandonedAt;  // abandonedAt?: string

    // --- Timer tracking ---
    @Builder.Default
    List<TimerEvent> timerEvents = new ArrayList<>(); // timerEvents: TimerEvent[]

    @Builder.Default
    List<ActiveTimer> activeTimers = new ArrayList<>(); // activeTimers: ActiveTimer[]

    // --- Completion data ---
    Integer rating;     // rating?: number
    String notes;       // notes?: string

    // --- XP tracking ---
    Double baseXpAwarded;       // baseXpAwarded?: number
    Double pendingXp;           // pendingXp?: number
    Double remainingXpAwarded;  // remainingXpAwarded?: number

    // --- Co-op multiplier (from co-cooking rooms) ---
    Double xpMultiplier;            // 1.0, 1.1, 1.2 — null for solo
    String xpMultiplierReason;      // "CO_OP_DUO", "CO_OP_GROUP", null

    // --- Link to post ---
    String postId;              // postId?: string
    LocalDateTime postDeadline; // postDeadline?: string
    LocalDateTime linkedAt;

    // --- Validation ---
    boolean flagged;        // flagged: boolean
    String flagReason;      // flagReason?: string

    // ==========================================
    // INNER CLASSES (POJOs)
    // ==========================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimerEvent {
        Integer stepNumber;     // stepNumber: number
        TimerEventType event;   // event: "start" | "complete" | "skip"
        LocalDateTime clientTimestamp; // clientTimestamp: string
        LocalDateTime serverTimestamp; // serverTimestamp: string
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActiveTimer {
        Integer stepNumber;     // stepNumber: number
        Integer totalSeconds;   // totalSeconds: number
        LocalDateTime startedAt;// startedAt: string
        Integer remainingSeconds; // remainingSeconds: number (Calculated snapshot)
    }
}