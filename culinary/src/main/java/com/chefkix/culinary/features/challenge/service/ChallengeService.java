package com.chefkix.culinary.features.challenge.service;

import com.chefkix.culinary.features.challenge.dto.response.ChallengeHistoryResponse;
import com.chefkix.culinary.features.challenge.dto.response.ChallengeResponse;
import com.chefkix.culinary.features.challenge.dto.response.ChallengeRewardResult;
import com.chefkix.culinary.features.challenge.entity.ChallengeLog;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.culinary.common.helper.StreakCalculatorHelper;
import com.chefkix.culinary.features.challenge.model.ChallengeDefinition;
import com.chefkix.culinary.features.challenge.repository.ChallengeLogRepository;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository; // Giả sử bạn đã có
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengeService {

    private final ChallengePoolService challengePoolService;
    private final ChallengeLogRepository challengeLogRepository;
    private final RecipeRepository recipeRepository;
    private final StreakCalculatorHelper streakCalculator;

    public ChallengeResponse getTodayChallenge(String userId) {
        // 1. Lấy Challenge hôm nay từ Pool
        ChallengeDefinition challenge = challengePoolService.getTodayChallenge();
        if (challenge == null) {
            throw new AppException(ErrorCode.CHALLENGE_NOT_FOUND);
        }

        // 2. Kiểm tra User đã hoàn thành chưa
        String todayStr = LocalDate.now(ZoneId.of("UTC")).toString(); // "2025-12-13"
        Optional<ChallengeLog> logOpt = challengeLogRepository.findByUserIdAndChallengeDate(userId, todayStr);

        boolean isCompleted = logOpt.isPresent();
        String completedAt = isCompleted ? logOpt.get().getCompletedAt().toString() : null;

        // 3. Tìm món ăn gợi ý (Matching Recipes)
        // Đây là phần logic tìm kiếm cơ bản dựa trên Metadata
        List<ChallengeResponse.RecipePreviewDto> matchingRecipes = findMatchingRecipes(challenge.getCriteriaMetadata());

        LocalDate today = LocalDate.now(ZoneId.of("UTC"));

        // B2: Cộng thêm 1 ngày -> Lấy 00:00:00 của ngày mai
        // Ví dụ: Hôm nay 13/12 -> Deadline là 14/12 lúc 00:00:00
        java.time.ZonedDateTime endOfDay = today.plusDays(1)
                .atStartOfDay(ZoneId.of("UTC"));

        // B3: Format sang chuẩn ISO 8601 (VD: "2025-12-14T00:00:00Z")
        String endsAtStr = endOfDay.format(java.time.format.DateTimeFormatter.ISO_INSTANT);
        // 4. Map sang DTO để trả về
        return ChallengeResponse.builder()
                .id(challenge.getId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .bonusXp(challenge.getBonusXp())
                .endsAt(endsAtStr)
                .criteria(challenge.getCriteriaMetadata()) // Trả về cục JSON criteria
                .completed(isCompleted)
                .completedAt(completedAt)
                .matchingRecipes(matchingRecipes) // Bạn nên map sang RecipePreviewDTO để gọn hơn
                .build();
    }

    /**
     * Logic tìm món ăn gợi ý dựa trên Metadata của Challenge
     * (Đây là ví dụ đơn giản, bạn có thể viết query Mongo phức tạp hơn)
     */
    /**
     * Logic tìm món ăn gợi ý
     */
    private List<ChallengeResponse.RecipePreviewDto> findMatchingRecipes(Map<String, Object> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            // Fallback: Nếu không có tiêu chí, trả về 5 món bất kỳ (hoặc rỗng)
            return Collections.emptyList();
        }

        List<Recipe> recipes = new ArrayList<>();

        // 1. Ưu tiên tìm theo Cuisine (Vì nó chính xác hơn)
        if (criteria.containsKey("cuisineType")) {
            List<String> cuisines = (List<String>) criteria.get("cuisineType");
            // Gọi Repository
            recipes = recipeRepository.findTop5ByCuisineTypeInIgnoreCase(cuisines);
        }

        // 2. Nếu chưa tìm thấy gì, thử tìm theo Nguyên liệu
        if (recipes.isEmpty() && criteria.containsKey("fullI")) {
            List<String> ingredients = (List<String>) criteria.get("ingredientContains");
            // Gọi Repository
            recipes = recipeRepository.findTop5ByFullIngredientListInIgnoreCase(ingredients);
        }

        // 3. Fallback: Nếu vẫn rỗng (do user nhập liệu sai hoặc không khớp)
        // Bạn có thể trả về list rỗng hoặc query findAll().stream().limit(3)... tùy ý.
        if (recipes.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. Map từ Entity (Recipe) sang DTO (RecipePreviewDto)
        return recipes.stream()
                .map(this::mapToPreviewDto)
                .toList(); // Java 16+
        // .collect(Collectors.toList()); // Nếu dùng Java cũ hơn
    }

    /**
     * Logic: Automatic Completion based on Specs
     * @param userId ID người dùng
     * @param recipe Món ăn vừa nấu xong
     * @return Kết quả thưởng (nếu có)
     */
    public Optional<ChallengeRewardResult> checkAndCompleteChallenge(String userId, Recipe recipe) {

        // 0. Lấy Challenge của hôm nay
        ChallengeDefinition challenge = challengePoolService.getTodayChallenge();
        if (challenge == null) return Optional.empty();

        String todayStr = LocalDate.now(ZoneId.of("UTC")).toString(); // "2025-12-13"

        // =================================================================
        // LOGIC 1: CHECK ĐÃ LÀM CHƯA? (Duplicate Check)
        // =================================================================
        // "hôm nay đã thực hiện 1 chellenge nào chưa"
        boolean alreadyCompleted = challengeLogRepository
                .existsByUserIdAndChallengeDate(userId, todayStr);

        if (alreadyCompleted) {
            return Optional.empty(); // Đã làm rồi -> Dừng lại, không thưởng nữa.
        }

        // =================================================================
        // LOGIC 2: CHECK RECIPE CÓ KHỚP KHÔNG? (Matching Check)
        // =================================================================
        // "check xem session đang thực hiện có recipeId khớp với recipe trong challenge"
        // (Ở đây ta check theo Criteria của challenge thay vì list ID cứng để linh hoạt hơn)
        if (challenge.isSatisfiedBy(recipe)) {

            // =============================================================
            // LOGIC 3: LƯU VÀO HISTORY (Save Log)
            // =============================================================
            // "nếu thỏa các điều kiện... thêm vào Challenge History"
            ChallengeLog historyLog = ChallengeLog.builder()
                    .userId(userId)
                    .challengeId(challenge.getId())
                    .challengeTitle(challenge.getTitle()) // Lưu Snapshot tên challenge
                    .recipeId(recipe.getId())
                    .recipeTitle(recipe.getTitle())       // Lưu Snapshot tên món ăn
                    .challengeDate(todayStr)
                    .bonusXp(challenge.getBonusXp())
                    .completedAt(Instant.now())
                    .build();

            try {
                challengeLogRepository.save(historyLog);

                // Trả về kết quả để báo cho Frontend
                return Optional.of(ChallengeRewardResult.builder()
                        .completed(true)
                        .bonusXp(challenge.getBonusXp())
                        .challengeTitle(challenge.getTitle())
                        .build());

            } catch (DuplicateKeyException e) {
                // Trường hợp 2 request chạy song song -> Chỉ tính 1 cái
                return Optional.empty();
            }
        }

        // Không khớp điều kiện -> Không thưởng
        return Optional.empty();
    }

    /**
     * Lấy lịch sử Challenge có phân trang
     * @param userId ID người dùng
     * @param page Số trang (bắt đầu từ 0)
     * @param size Số lượng item mỗi trang (limit)
     */
    @Transactional(readOnly = true)
    public ChallengeHistoryResponse getChallengeHistory(String userId, int page, int size) {

        // 1. Query DB lấy Page (để limit số lượng record trả về)
        Pageable pageable = PageRequest.of(page, size, Sort.by("challengeDate").descending());
        Page<ChallengeLog> historyPage = challengeLogRepository.findByUserId(userId, pageable);

        // 2. Map Entity sang DTO
        // Lưu ý: Kết quả của dòng này là List<Dto>, KHÔNG PHẢI Page<Dto>
        List<ChallengeHistoryResponse.ChallengeItemDto> challengesList = historyPage.getContent() // <--- BÓC LẤY LIST
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        List<String> allDateStrings = challengeLogRepository.findCompletedDatesByUserId(userId);

        // Convert List<String> -> List<LocalDate>
        List<LocalDate> allDates = allDateStrings.stream()
                .map(LocalDate::parse)
                .collect(Collectors.toList());

        var streakResult = streakCalculator.calculate(allDates);
        var sumResult = challengeLogRepository.sumBonusXpByUserId(userId);
        long totalBonusXp = (sumResult != null) ? sumResult.totalXp : 0;

        // 4. Đóng gói Response (Chỉ List và Stats)
        return ChallengeHistoryResponse.builder()
                .challenges(challengesList) // Truyền List vào
                .stats(ChallengeHistoryResponse.StatsDto.builder()
                        .totalCompleted((long) allDateStrings.size())
                        .currentStreak(streakResult.getCurrentStreak())
                        .longestStreak(streakResult.getLongestStreak())
                        .totalBonusXp(totalBonusXp)
                        .build())
                .build();
    }

    /**
     * Helper: Chuyển đổi Recipe Entity -> DTO gọn nhẹ
     */
    private ChallengeResponse.RecipePreviewDto mapToPreviewDto(Recipe recipe) {
        return ChallengeResponse.RecipePreviewDto.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .xpReward(recipe.getXpReward()) // Hàm tính XP của bạn, hoặc lấy field xpReward có sẵn
                .coverImageUrl(recipe.getCoverImageUrl()) // Sửa getter cho đúng với entity của bạn
                .totalTime(recipe.getTotalTimeMinutes())
                .difficulty(recipe.getDifficulty())
                .build();
    }

    // --- Helper Mapping (Fixed for ChallengeLog) ---
    private ChallengeHistoryResponse.ChallengeItemDto mapToDto(ChallengeLog log) {
        ChallengeHistoryResponse.RecipeShortInfo recipeInfo = null;

        if (StringUtils.hasText(log.getRecipeId())) {
            // Check cache or simple query
            recipeInfo = ChallengeHistoryResponse.RecipeShortInfo.builder()
                    .id(log.getRecipeId())
                    .title(log.getRecipeTitle()) // ChallengeLog already has the title snapshot
                    .build();
        }

        return ChallengeHistoryResponse.ChallengeItemDto.builder()
                .id(log.getChallengeId())
                .title(log.getChallengeTitle())
                .date(LocalDate.parse(log.getChallengeDate())) // Convert String back to LocalDate
                .completed(true) // If it's in the log, it is completed
                .completedAt(LocalDateTime.ofInstant(log.getCompletedAt(), ZoneId.of("UTC")))
                .bonusXpEarned(log.getBonusXp())
                .recipeCooked(recipeInfo)
                .build();
    }
}