package com.chefkix.culinary.features.cookplan.dto.request;

import com.chefkix.culinary.features.cookplan.entity.CookPlanMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCookPlanRequest {

    @NotNull
    private LocalDate planDate;

    @NotNull
    private CookPlanMode mode;

    @Min(1)
    @Max(12)
    private int householdSize;

    @Min(15)
    @Max(240)
    private int maxActiveMinutes;

    private boolean pantryFirst;
}
