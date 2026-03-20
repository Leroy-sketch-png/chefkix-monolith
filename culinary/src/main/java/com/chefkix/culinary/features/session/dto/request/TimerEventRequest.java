package com.chefkix.culinary.features.session.dto.request;
import com.chefkix.culinary.common.enums.TimerEventType; // START, COMPLETE, SKIP
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TimerEventRequest {
    @NotNull(message = "stepNumber is required")
    @Min(value = 1, message = "stepNumber must be at least 1")
    private Integer stepNumber;

    @NotNull(message = "event is required")
    private TimerEventType event;

    @NotNull(message = "clientTimestamp is required")
    private LocalDateTime clientTimestamp;
}