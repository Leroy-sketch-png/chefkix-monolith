package com.chefkix.culinary.features.session.service;

import com.chefkix.identity.api.dto.CompletionRequest;
import com.chefkix.culinary.features.session.dto.response.CompletionResponse;
import com.chefkix.culinary.features.recipe.dto.request.RecipeCompletionRequest;
import com.chefkix.identity.api.dto.CompletionResult;
import com.chefkix.culinary.features.session.dto.internal.SessionResponse;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.entity.RecipeCompletion;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.culinary.features.recipe.repository.CompletionRepository;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.identity.api.ProfileProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GamificationService {

    RecipeRepository recipeRepository;
    CompletionRepository completionRepository;
    ProfileProvider profileProvider;

    @Transactional
    public CompletionResponse completeRecipe(String recipeId, RecipeCompletionRequest request) {
        // 1. LẤY USER ID TỪ TOKEN (Bảo mật - Server tự quyết định danh tính)
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDate today = LocalDate.now();

        // 2. TÌM RECIPE GỐC
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        // 3. RATE LIMIT CHECK (Logic chống spam 5 lần/ngày)
        long completedToday = completionRepository.countByUserIdAndCompletedAtAfter(
                userId, today.atStartOfDay());

        // Account age-based rate limiting: new accounts (< 7 days) get stricter limits
        java.time.Instant accountCreatedAt = profileProvider.getAccountCreatedAt(userId);
        long accountAgeDays = accountCreatedAt != null
                ? java.time.temporal.ChronoUnit.DAYS.between(accountCreatedAt, java.time.Instant.now())
                : Long.MAX_VALUE;
        int dailyLimit = accountAgeDays < 7 ? 2 : 5;
        if (completedToday >= dailyLimit) {
            throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED,
                    "Bạn đã đạt giới hạn nấu ăn trong ngày (" + dailyLimit + " món).");
        }

        // 4. TIME VALIDATION (Anti-Cheat)
        long totalElapsedSeconds = request.getTimerLogs() != null
                ? request.getTimerLogs().stream()
                    .mapToLong(RecipeCompletionRequest.TimerLog::getElapsedSeconds)
                    .sum()
                : 0;
        validateCookingTime(recipe.getTotalTimeMinutes(), totalElapsedSeconds);

        // 5. LOGIC TÍNH ĐIỂM (HYBRID XP & BADGE)
        // Kiểm tra xem user có gửi ảnh bằng chứng không
        boolean hasProof = request.getProofImageUrls() != null && !request.getProofImageUrls().isEmpty();

        int finalXp;
        boolean isPublic;
        List<String> badgesToAward = new ArrayList<>();

        if (hasProof) {
            // CASE A: CÓ ẢNH -> Full XP + Có cơ hội nhận Badge
            finalXp = recipe.getXpReward();
            isPublic = true;

            // Chỉ tặng Badge nếu recipe có cấu hình badge
            if (recipe.getRewardBadges() != null && !recipe.getRewardBadges().isEmpty()) {
                badgesToAward.addAll(recipe.getRewardBadges());
            }
            log.info("[COMPLETION] User {} hoàn thành PUBLIC. Full XP.", userId);
        } else {
            // CASE B: KHÔNG ẢNH -> Half XP + Không Badge
            finalXp = (int) Math.ceil(recipe.getXpReward() * 0.5);
            isPublic = false;
            log.info("[COMPLETION] User {} hoàn thành PRIVATE. Half XP.", userId);
        }

        // 6. LƯU LỊCH SỬ (Vào MongoDB của Recipe Service)
        RecipeCompletion completion = RecipeCompletion.builder()
                .userId(userId)
                .recipeId(recipeId)
                .proofImageUrls(request.getProofImageUrls()) // Lưu ảnh từ request
                .actualDurationSeconds((int) totalElapsedSeconds)
                .xpAwarded(finalXp)      // Lưu số XP thực nhận
                .isPublic(isPublic)
                .completedAt(LocalDateTime.now())
                .build();
        completionRepository.save(completion);

        // 7. GỌI PROFILE SERVICE (Sync)
        // Tạo DTO Internal để gửi đi (Điền các thông tin Server đã tính toán)
        CompletionRequest completionRequest = CompletionRequest.builder()
                .userId(userId)
                .xpAmount(finalXp)
                .newBadges(badgesToAward)
                .build();

        CompletionResult profileResult;
        try {
            // Direct method call — no Feign wrapping
            profileResult = profileProvider.updateAfterCompletion(completionRequest);

            if (profileResult == null) {
                log.error("Profile Service returned null data for user {}", userId);
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Không nhận được dữ liệu từ Profile Service");
            }
        } catch (Exception e) {
            log.error("Lỗi kết nối Profile Service: ", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Lỗi đồng bộ hồ sơ người dùng");
        }

        // 8. MAP DỮ LIỆU VÀ TRẢ VỀ (Cho Frontend)
        // Ghép data của lần nấu này + data user mới nhất từ Profile Service
        return CompletionResponse.builder()
                .completionId(completion.getId())
                .recipeId(recipeId)
                .xpEarned(finalXp)
                .newBadges(badgesToAward)
                .userProfile(CompletionResult.builder()
                        .userId(userId)
                        .currentXP(profileResult.getCurrentXP())
                        .currentXPGoal(profileResult.getCurrentXPGoal())
                        .currentLevel(profileResult.getCurrentLevel())
                        .completionCount(profileResult.getCompletionCount())
                        .build())
                .build();
    }

    public SessionResponse getSessionById(String sessionId) {
        // 1. Tìm Session nấu ăn (RecipeCompletion)
        RecipeCompletion session = completionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.COMPLETION_NOT_FOUND));

        // 2. Tìm Recipe gốc (Để lấy thông tin tác giả và Title)
        Recipe recipe = recipeRepository.findById(session.getRecipeId())
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        // 3. Map sang DTO trả về cho Post Service
        return SessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .completedAt(session.getCompletedAt())

                // Lấy số XP user đã nhận được khi nấu xong (Pending XP)
                .pendingXp((double) session.getXpAwarded())

                // Thông tin Recipe
                .recipeId(recipe.getId())
                .recipeTitle(recipe.getTitle())

                // Quan trọng: Lấy ID tác giả để Post Service tính bonus 4%
                .recipeAuthorId(recipe.getUserId())
                .recipeBaseXp((double) recipe.getXpReward())
                .build();
    }

    // --- Helper Methods ---

    private void validateCookingTime(int recipeMinutes, long actualSeconds) {
        double actualMinutes = actualSeconds / 60.0;
        // Cho phép nhanh hơn tối đa 50% (ví dụ món 60p mà nấu 20p là cheat)
        double minTime = recipeMinutes * 0.5;

        if (actualMinutes < minTime) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    String.format("Thời gian nấu quá nhanh (%d phút). Cần tối thiểu %.0f phút.",
                            (int)actualMinutes, minTime));
        }
    }
}