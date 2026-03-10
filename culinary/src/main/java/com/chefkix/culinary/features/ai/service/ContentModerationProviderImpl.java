package com.chefkix.culinary.features.ai.service;

import com.chefkix.culinary.api.ContentModerationProvider;
import com.chefkix.culinary.common.client.AIRestClient;
import com.chefkix.culinary.features.ai.dto.internal.AIModerationRequest;
import com.chefkix.culinary.features.ai.dto.internal.AIModerationResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation of ContentModerationProvider SPI.
 * Wraps AIRestClient for cross-module AI moderation.
 *
 * Fail-open for non-recipe content: if AI service is down,
 * posts/comments/chat are allowed through with a warning log.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContentModerationProviderImpl implements ContentModerationProvider {

    AIRestClient aiRestClient;

    @Override
    public ModerationResult moderate(String content, String contentType) {
        if (content == null || content.isBlank()) {
            return ModerationResult.approved();
        }

        try {
            AIModerationRequest request = AIModerationRequest.builder()
                    .content(content)
                    .contentType(contentType)
                    .build();

            AIModerationResponse response = aiRestClient.moderateContent(request);

            log.info("Content moderation result for {}: action={}, category={}, confidence={}",
                    contentType, response.getAction(), response.getCategory(), response.getConfidence());

            return new ModerationResult(
                    response.getAction(),
                    response.getCategory(),
                    response.getSeverity(),
                    response.getConfidence(),
                    response.getReason()
            );
        } catch (Exception e) {
            // Fail-open for non-critical content
            log.warn("AI moderation unavailable for {} content — allowing through: {}",
                    contentType, e.getMessage());
            return ModerationResult.approved();
        }
    }
}
