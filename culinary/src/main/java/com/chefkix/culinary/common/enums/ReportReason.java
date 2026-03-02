package com.chefkix.culinary.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ReportReason {
    FRAUD("fraud"),             // Gian lận (ảnh giả)
    INAPPROPRIATE("inappropriate"),  // Nội dung không phù hợp
    SPAM("spam"),               // Spam
    HARASSMENT("harassment"),   // Quấy rối
    COPYRIGHT("copyright"),     // Vi phạm bản quyền
    OTHER("other");             // Lý do khác

    private final String value;

    ReportReason(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}