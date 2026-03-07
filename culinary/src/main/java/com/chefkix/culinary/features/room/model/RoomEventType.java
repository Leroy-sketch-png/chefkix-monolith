package com.chefkix.culinary.features.room.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RoomEventType {
    PARTICIPANT_JOINED("PARTICIPANT_JOINED"),
    PARTICIPANT_LEFT("PARTICIPANT_LEFT"),
    STEP_NAVIGATED("STEP_NAVIGATED"),
    STEP_COMPLETED("STEP_COMPLETED"),
    TIMER_STARTED("TIMER_STARTED"),
    TIMER_COMPLETED("TIMER_COMPLETED"),
    REACTION("REACTION"),
    SESSION_COMPLETED("SESSION_COMPLETED"),
    HOST_TRANSFERRED("HOST_TRANSFERRED"),
    ROOM_DISSOLVED("ROOM_DISSOLVED");

    private final String value;

    RoomEventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
