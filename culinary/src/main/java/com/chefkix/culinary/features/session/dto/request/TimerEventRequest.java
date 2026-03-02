package com.chefkix.culinary.features.session.dto.request;
import com.chefkix.culinary.common.enums.TimerEventType; // START, COMPLETE, SKIP
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TimerEventRequest {
    private Integer stepNumber;
    private TimerEventType event;
    private LocalDateTime clientTimestamp;
}