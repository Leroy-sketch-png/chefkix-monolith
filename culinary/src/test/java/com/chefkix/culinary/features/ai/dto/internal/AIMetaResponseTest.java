package com.chefkix.culinary.features.ai.dto.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AIMetaResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesCamelCaseMetasPayloadFromAiService() throws Exception {
        String json = """
                {
                  "xpReward": 104,
                  "xpBreakdown": {
                    "base": 50,
                    "baseReason": "Beginner difficulty base reward",
                    "steps": 40,
                    "stepsReason": "4 steps x 10 XP",
                    "time": 14,
                    "timeReason": "7 minutes x 2 XP",
                    "techniques": null,
                    "techniquesReason": null,
                    "total": 104
                  },
                  "difficultyMultiplier": 1.0,
                  "badges": ["Newbie Chef"],
                  "skillTags": ["Mixing"],
                  "equipmentNeeded": ["Pan", "Whisk"],
                  "ingredientSubstitutions": {
                    "eggs": ["flax egg", "chia egg"]
                  },
                  "culturalContext": {
                    "region": "Western Europe",
                    "tradition": "Classical techniques"
                  },
                  "recipeStory": "Story",
                  "chefNotes": "Notes",
                  "aiEnriched": true,
                  "xpValidated": true,
                  "validationConfidence": 1.0,
                  "validationIssues": [],
                  "xpAdjusted": false,
                  "aiUsed": true
                }
                """;

        AIMetaResponse response = objectMapper.readValue(json, AIMetaResponse.class);

        assertThat(response.getXpReward()).isEqualTo(104);
        assertThat(response.getDifficultyMultiplier()).isEqualTo(1.0);
        assertThat(response.getXpBreakdown()).isNotNull();
        assertThat(response.getXpBreakdown().getTotal()).isEqualTo(104);
        assertThat(response.getSkillTags()).containsExactly("Mixing");
        assertThat(response.isXpValidated()).isTrue();
        assertThat(response.getValidationConfidence()).isEqualTo(1.0);
        assertThat(response.getCulturalContext()).isNotNull();
        assertThat(response.getCulturalContext().getRegion()).isEqualTo("Western Europe");
        assertThat(response.getCulturalContext().getBackground()).isEqualTo("Classical techniques");
    }
}