package com.chefkix.culinary.features.session.dto.response;

import com.chefkix.identity.api.dto.CompletionResult;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompletionResponse {

    String completionId;
    String recipeId;

int xpEarned;
List<String> newBadges;
CompletionResult userProfile;

//
//
}