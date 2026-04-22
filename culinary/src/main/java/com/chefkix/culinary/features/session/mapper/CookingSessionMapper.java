package com.chefkix.culinary.features.session.mapper;

import com.chefkix.culinary.features.session.dto.response.SessionHistoryResponse;
import com.chefkix.culinary.features.session.dto.response.StartSessionResponse;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.common.enums.SessionStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring") // Registered as Spring Bean
public interface CookingSessionMapper {

    // Instance for unit tests or non-Spring-Bean contexts
    CookingSessionMapper INSTANCE = Mappers.getMapper(CookingSessionMapper.class);

    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "status", source = "session.status", qualifiedByName = "mapStatusToString")
    @Mapping(target = "totalSteps", source = "recipe", qualifiedByName = "mapTotalSteps")
    @Mapping(target = "activeTimers", source = "session.activeTimers", qualifiedByName = "mapActiveTimers")
    @Mapping(target = "recipe", source = "recipe")
    StartSessionResponse toStartSessionResponse(CookingSession session, Recipe recipe);


    // Mapping for nested RecipeInfo
    @Mapping(target = "id", source = "id")
    StartSessionResponse.RecipeInfo mapRecipeToRecipeInfo(Recipe recipe);


    // --- Custom Mappers ---

    // Convert Status Enum to lowercase string (in_progress)
    @Named("mapStatusToString")
    default String mapStatusToString(SessionStatus status) {
        return status != null ? status.name().toLowerCase() : null;
    }

    // Calculate total number of steps from List<Step>
    @Named("mapTotalSteps")
    default Integer mapTotalSteps(Recipe recipe) {
        return (recipe != null && recipe.getSteps() != null) ? recipe.getSteps().size() : 0;
    }

    // Ensure activeTimers is never null and has correct type List<Object>
    @Named("mapActiveTimers")
    default List<Object> mapActiveTimers(List<CookingSession.ActiveTimer> activeTimers) {
        if (activeTimers == null) {
            return new ArrayList<>(); // Return empty array to match spec
        }
        // Since ActiveTimer is an inner class of Entity, MapStruct can auto-map List<ActiveTimer> to List<Object>
        return new ArrayList<>(activeTimers);
    }

        // ====================================================================
        // 1. MAIN MAPPING: Entity -> SessionItemDto
        // ====================================================================

        @Mapping(target = "sessionId", source = "session.id")
        @Mapping(target = "xpEarned", source = "session", qualifiedByName = "calculateXpEarned")
        @Mapping(target = "daysRemaining", source = "session", qualifiedByName = "calculateDaysRemaining")
        @Mapping(target = "baseXpAwarded", source = "session", qualifiedByName = "roundBaseXp")
        @Mapping(target = "pendingXp", source = "session", qualifiedByName = "roundPendingXp")

        // The following fields need to be fetched from another Entity (Recipe) OR from Cache/Denormalization.
        // Temporarily left as a separate method or requires passing in the Recipe Entity.
        @Mapping(target = "recipeTitle", source = "recipeTitle")
        @Mapping(target = "coverImageUrl", source = "coverImageUrl")
        SessionHistoryResponse.SessionItemDto toSessionItemDto(CookingSession session);

        /**
         * Round base XP to integer for clean game system values.
         */
        @Named("roundBaseXp")
        default Integer roundBaseXp(CookingSession session) {
            return session.getBaseXpAwarded() != null ? (int) Math.round(session.getBaseXpAwarded()) : null;
        }

        /**
         * Round pending XP to integer for clean game system values.
         */
        @Named("roundPendingXp")
        default Integer roundPendingXp(CookingSession session) {
            return session.getPendingXp() != null ? (int) Math.round(session.getPendingXp()) : null;
        }


        // ====================================================================
        // 2. BUSINESS LOGIC CALCULATIONS (Custom Methods)
        // ====================================================================

        /**
         * Calculate total XP earned (base + pending) if session has been POSTED.
         * Returns Integer for clean game system values.
         */
        @Named("calculateXpEarned")
        default Integer calculateXpEarned(CookingSession session) {
            if (session.getStatus() != null && session.getStatus().hasClaimedPostXp()) {
                Double base = session.getBaseXpAwarded() != null ? session.getBaseXpAwarded() : 0.0;
                Double remaining = session.getRemainingXpAwarded() != null ? session.getRemainingXpAwarded() : 0.0;
                // If using pendingXp instead of remainingXpAwarded, change to: base + (session.getPendingXp() != null ? session.getPendingXp() : 0.0);
                return (int) Math.round(base + remaining);
            }
            return null;
        }

        /**
         * Calculate remaining days to create a post (only applies to status=COMPLETED).
         */
        @Named("calculateDaysRemaining")
        default Long calculateDaysRemaining(CookingSession session) {
            if (session.getStatus() == SessionStatus.COMPLETED && session.getPostDeadline() != null) {
                LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                long days = ChronoUnit.DAYS.between(now, session.getPostDeadline());
                return Math.max(0, days); // Return 0 if deadline has passed
            }
            return null;
        }

        // ====================================================================
        // 3. CROSS-DATA MAPPING (Required for RecipeTitle/CoverImage)
        // ====================================================================
        /*
         * In practice, you will need another method that takes both CookingSession and Recipe (or Map<String, Recipe>)
         * to populate the recipeTitle and coverImageUrl fields.
         */

        // Example of a more complex method (if MapStruct needs to handle cross-data mapping)
        // @Mapping(target = "recipeTitle", source = "recipe.title")
        // SessionHistoryResponse.SessionItemDto toSessionItemDto(CookingSession session, Recipe recipe);
    }