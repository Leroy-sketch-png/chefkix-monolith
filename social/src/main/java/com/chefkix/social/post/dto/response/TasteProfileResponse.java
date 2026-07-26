package com.chefkix.social.post.dto.response;

import java.util.List;
import java.util.Map;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TasteProfileResponse {

    Map<String, Double> tasteVector;

    List<CuisineBreakdown> cuisineDistribution;

    int totalInteractions;

    List<String> topCuisines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CuisineBreakdown {
        String cuisine;
        double percentage;
        int interactionCount;
    }
}
