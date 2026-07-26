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

@Mapper(componentModel = "spring")
public interface CookingSessionMapper {

    CookingSessionMapper INSTANCE = Mappers.getMapper(CookingSessionMapper.class);

    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "status", source = "session.status", qualifiedByName = "mapStatusToString")
    @Mapping(target = "totalSteps", source = "recipe", qualifiedByName = "mapTotalSteps")
    @Mapping(target = "activeTimers", source = "session.activeTimers", qualifiedByName = "mapActiveTimers")
    @Mapping(target = "recipe", source = "recipe")
    StartSessionResponse toStartSessionResponse(CookingSession session, Recipe recipe);


    @Mapping(target = "id", source = "id")
    StartSessionResponse.RecipeInfo mapRecipeToRecipeInfo(Recipe recipe);


    @Named("mapStatusToString")
    default String mapStatusToString(SessionStatus status) {
        return status != null ? status.name().toLowerCase() : null;
    }

    @Named("mapTotalSteps")
    default Integer mapTotalSteps(Recipe recipe) {
        return (recipe != null && recipe.getSteps() != null) ? recipe.getSteps().size() : 0;
    }

    @Named("mapActiveTimers")
    default List<Object> mapActiveTimers(List<CookingSession.ActiveTimer> activeTimers) {
        if (activeTimers == null) {
return new ArrayList<>();
        }
        return new ArrayList<>(activeTimers);
    }


        @Mapping(target = "sessionId", source = "session.id")
        @Mapping(target = "xpEarned", source = "session", qualifiedByName = "calculateXpEarned")
        @Mapping(target = "daysRemaining", source = "session", qualifiedByName = "calculateDaysRemaining")
        @Mapping(target = "baseXpAwarded", source = "session", qualifiedByName = "roundBaseXp")
        @Mapping(target = "pendingXp", source = "session", qualifiedByName = "roundPendingXp")

        @Mapping(target = "recipeTitle", source = "recipeTitle")
        @Mapping(target = "coverImageUrl", source = "coverImageUrl")
        SessionHistoryResponse.SessionItemDto toSessionItemDto(CookingSession session);

        /**
         */
        @Named("roundBaseXp")
        default Integer roundBaseXp(CookingSession session) {
            return session.getBaseXpAwarded() != null ? (int) Math.round(session.getBaseXpAwarded()) : null;
        }

        /**
         */
        @Named("roundPendingXp")
        default Integer roundPendingXp(CookingSession session) {
            return session.getPendingXp() != null ? (int) Math.round(session.getPendingXp()) : null;
        }


        /**
         */
        @Named("calculateXpEarned")
        default Integer calculateXpEarned(CookingSession session) {
            if (session.getStatus() != null && session.getStatus().hasClaimedPostXp()) {
                Double base = session.getBaseXpAwarded() != null ? session.getBaseXpAwarded() : 0.0;
                Double remaining = session.getRemainingXpAwarded() != null ? session.getRemainingXpAwarded() : 0.0;
                return (int) Math.round(base + remaining);
            }
            return null;
        }

        /**
         */
        @Named("calculateDaysRemaining")
        default Long calculateDaysRemaining(CookingSession session) {
            if (session.getStatus() == SessionStatus.COMPLETED && session.getPostDeadline() != null) {
                LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                long days = ChronoUnit.DAYS.between(now, session.getPostDeadline());
return Math.max(0, days);
            }
            return null;
        }

        /*
         */

    }