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

    /** Top tags with normalized weights (0..1). Ordered by weight descending. */
    Map<String, Double> tasteVector;

    /** Cuisine distribution: cuisine name → percentage (0-100). Top 10. */
    List<CuisineBreakdown> cuisineDistribution;

    /** Total interactions that built this profile (likes + saves + views + dwells) */
    int totalInteractions;

    /** Top 3 cuisine names for quick display */
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
