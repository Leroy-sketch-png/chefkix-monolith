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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring") // Đăng ký là Spring Bean
public interface CookingSessionMapper {

    // instance dùng cho unit test hoặc nơi không phải Spring Bean
    CookingSessionMapper INSTANCE = Mappers.getMapper(CookingSessionMapper.class);

    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "status", source = "session.status", qualifiedByName = "mapStatusToString")
    @Mapping(target = "totalSteps", source = "recipe", qualifiedByName = "mapTotalSteps")
    @Mapping(target = "activeTimers", source = "session.activeTimers", qualifiedByName = "mapActiveTimers")
    @Mapping(target = "recipe", source = "recipe")
    StartSessionResponse toStartSessionResponse(CookingSession session, Recipe recipe);


    // Mapping cho RecipeInfo lồng bên trong
    @Mapping(target = "id", source = "id")
    StartSessionResponse.RecipeInfo mapRecipeToRecipeInfo(Recipe recipe);


    // --- Custom Mappers ---

    // Chuyển Enum Status sang chuỗi lower case (in_progress)
    @Named("mapStatusToString")
    default String mapStatusToString(SessionStatus status) {
        return status != null ? status.name().toLowerCase() : null;
    }

    // Tính tổng số bước từ List<Step>
    @Named("mapTotalSteps")
    default Integer mapTotalSteps(Recipe recipe) {
        return (recipe != null && recipe.getSteps() != null) ? recipe.getSteps().size() : 0;
    }

    // Đảm bảo activeTimers không bao giờ null và đúng kiểu List<Object>
    @Named("mapActiveTimers")
    default List<Object> mapActiveTimers(List<CookingSession.ActiveTimer> activeTimers) {
        if (activeTimers == null) {
            return new ArrayList<>(); // Trả về mảng rỗng để khớp spec
        }
        // Vì ActiveTimer là inner class của Entity, MapStruct tự map được List<ActiveTimer> sang List<Object>
        return new ArrayList<>(activeTimers);
    }

        // ====================================================================
        // 1. MAPPING CHÍNH: Entity -> SessionItemDto
        // ====================================================================

        @Mapping(target = "sessionId", source = "session.id")
        @Mapping(target = "xpEarned", source = "session", qualifiedByName = "calculateXpEarned")
        @Mapping(target = "daysRemaining", source = "session", qualifiedByName = "calculateDaysRemaining")
        @Mapping(target = "baseXpAwarded", source = "session", qualifiedByName = "roundBaseXp")
        @Mapping(target = "pendingXp", source = "session", qualifiedByName = "roundPendingXp")

        // Các trường sau đây cần lấy từ một Entity khác (Recipe) HOẶC từ Cache/Denormalization.
        // Tạm thời để là một phương thức riêng biệt hoặc cần truyền Recipe Entity vào.
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
        // 2. LOGIC TÍNH TOÁN NGHIỆP VỤ (Custom Methods)
        // ====================================================================

        /**
         * Tính toán tổng XP kiếm được (base + pending) nếu session đã POSTED.
         * Returns Integer for clean game system values.
         */
        @Named("calculateXpEarned")
        default Integer calculateXpEarned(CookingSession session) {
            if (session.getStatus() == SessionStatus.POSTED) {
                Double base = session.getBaseXpAwarded() != null ? session.getBaseXpAwarded() : 0.0;
                Double remaining = session.getRemainingXpAwarded() != null ? session.getRemainingXpAwarded() : 0.0;
                // Nếu dùng pendingXp thay vì remainingXpAwarded, sửa thành: base + (session.getPendingXp() != null ? session.getPendingXp() : 0.0);
                return (int) Math.round(base + remaining);
            }
            return null;
        }

        /**
         * Tính toán số ngày còn lại để đăng bài (chỉ áp dụng cho status=COMPLETED).
         */
        @Named("calculateDaysRemaining")
        default Long calculateDaysRemaining(CookingSession session) {
            if (session.getStatus() == SessionStatus.COMPLETED && session.getPostDeadline() != null) {
                LocalDateTime now = LocalDateTime.now();
                long days = ChronoUnit.DAYS.between(now, session.getPostDeadline());
                return Math.max(0, days); // Trả về 0 nếu đã quá hạn
            }
            return null;
        }

        // ====================================================================
        // 3. MAPPING CÓ DỮ LIỆU CHÉO (Cần thiết cho RecipeTitle/CoverImage)
        // ====================================================================
        /*
         * Trong thực tế, bạn sẽ cần một hàm khác nhận cả CookingSession và Recipe (hoặc Map<String, Recipe>)
         * để điền các trường recipeTitle và coverImageUrl.
         */

        // Ví dụ về một phương thức phức tạp hơn (nếu bạn cần MapStruct xử lý data chéo)
        // @Mapping(target = "recipeTitle", source = "recipe.title")
        // SessionHistoryResponse.SessionItemDto toSessionItemDto(CookingSession session, Recipe recipe);
    }