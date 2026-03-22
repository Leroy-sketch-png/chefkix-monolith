package com.chefkix.social.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlateRateRequest {
    @NotBlank
    @Pattern(regexp = "^(FIRE|CRINGE)$", message = "Rating must be FIRE or CRINGE")
    String rating;
}
