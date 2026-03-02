package com.chefkix.culinary.features.recipe.service;

import com.chefkix.culinary.features.recipe.dto.request.RecipePublishRequest;
import com.chefkix.culinary.features.recipe.dto.request.RecipeRequest;
import com.chefkix.culinary.common.dto.response.AuthorResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeDetailResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipePublishResponse;
import com.chefkix.culinary.features.recipe.dto.response.RecipeSummaryResponse;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.culinary.common.helper.AsyncHelper;
import com.chefkix.culinary.features.recipe.mapper.IngredientMapper;
import com.chefkix.culinary.features.recipe.mapper.RecipeMapper;
import com.chefkix.culinary.features.recipe.mapper.StepMapper;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class DraftService {

    private final RecipeRepository recipeRepository;
    private final RecipeMapper mapper;
    private final StepMapper stepMapper;
    private final IngredientMapper ingredientMapper;
    private final AsyncHelper asyncHelper;

    @Transactional
    public RecipeDetailResponse createDraft() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. Gọi Async Author sớm (Tận dụng thời gian chờ DB để lấy user info)
        // Frontend cần cái này để hiển thị avatar "Người soạn: Nguyễn Văn A" ngay lúc mở màn hình
        CompletableFuture<AuthorResponse> authorFuture = asyncHelper.getProfileAsync(userId);

        // 2. Tạo Entity RỖNG (Bare-bones)
        Recipe draft = Recipe.builder()
                .userId(userId)
                .status(RecipeStatus.DRAFT) // <--- QUAN TRỌNG NHẤT
                .createdAt(Instant.now())
                .updatedAt(Instant.now())

                // Init sẵn các list rỗng để về FE không bị null pointer
                .coverImageUrl(new ArrayList<>())
                .fullIngredientList(new ArrayList<>())
                .steps(new ArrayList<>())

                // Init các chỉ số 0
                .likeCount(0)
                .cookCount(0)
                .build();

        // 3. Save để sinh ID (Lúc này Mongo sẽ tạo field _id)
        draft = recipeRepository.save(draft);

        // 4. Map sang Response
        RecipeDetailResponse response = mapper.toRecipeDetailResponse(draft);
        response.setRecipeStatus(RecipeStatus.DRAFT);

        // 5. Join Author (Lấy kết quả Async)
        // Thời gian save DB ~10-20ms, thời gian gọi Identity ~15-30ms
        // -> Chạy song song giúp tiết kiệm thời gian
        response.setAuthor(authorFuture.join());

        // Draft thì chắc chắn chưa ai like/save
        response.setIsLiked(false);
        response.setIsSaved(false);

        return response;
    }

    // =========================================================================
    // 2. AUTO-SAVE (Logic Update mềm dẻo)
    // =========================================================================
    @Transactional
    public RecipeDetailResponse autoSaveDraft(String id, RecipeRequest request) {
        // 1. Tìm bài viết
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        // 2. Check quyền chủ sở hữu (Security)
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!recipe.getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 3. Map dữ liệu (QUAN TRỌNG: Chỉ map các trường khác null)
        // Nếu FE chỉ gửi title, thì description, steps... cũ phải giữ nguyên
        mapper.updateRecipeFromRequest(recipe, request);

        // 4. EXPLICIT COLLECTION HANDLING (MapStruct doesn't handle list replacement well)
        // Steps: REPLACE the entire list if provided
        if (request.getSteps() != null) {
            var newSteps = request.getSteps().stream()
                    .map(stepMapper::toStep)
                    .toList();
            recipe.getSteps().clear();
            recipe.getSteps().addAll(newSteps);
            log.debug("Updated {} steps for draft {}", newSteps.size(), id);
        }

        // Ingredients: REPLACE the entire list if provided  
        if (request.getFullIngredientList() != null) {
            var newIngredients = request.getFullIngredientList().stream()
                    .map(ingredientMapper::toIngredient)
                    .toList();
            recipe.getFullIngredientList().clear();
            recipe.getFullIngredientList().addAll(newIngredients);
            log.debug("Updated {} ingredients for draft {}", newIngredients.size(), id);
        }

        // 5. Update Metadata
        recipe.setUpdatedAt(Instant.now());

        // 6. Lưu (Không thay đổi status, không validate business rules)
        recipe = recipeRepository.save(recipe);

        return mapper.toRecipeDetailResponse(recipe);
    }

    // =========================================================================
    // 3. GET DRAFTS & DELETE
    // =========================================================================
    public List<RecipeSummaryResponse> getMyDrafts() {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. Bắn Async Request lấy Profile NGAY LẬP TỨC
        // (Để nó chạy song song trong lúc đang query Database ở bước 2)
        CompletableFuture<AuthorResponse> authorFuture = asyncHelper.getProfileAsync(currentUserId);

        // 2. Query Database (IO Blocking)
        List<Recipe> drafts = recipeRepository
                .findByUserIdAndStatusOrderByUpdatedAtDesc(currentUserId, RecipeStatus.DRAFT);

        if (drafts.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Chốt kết quả Author (JOIN)
        // Lúc này DB đã query xong, khả năng cao là authorFuture cũng đã có kết quả -> Không phải chờ lâu
        AuthorResponse authorProfile = authorFuture.join();

        // 4. Map & Enrich
        return drafts.stream()
                .map(draft -> {
                    // Map cơ bản
                    RecipeSummaryResponse response = mapper.toRecipeSummaryResponse(draft);

                    // Gán Author (Dùng chung object authorProfile cho tất cả item -> Tiết kiệm memory)
                    if (authorProfile != null) {
                        response.setAuthor(AuthorResponse.builder()
                                .userId(authorProfile.getUserId())
                                .displayName(authorProfile.getDisplayName())
                                .avatarUrl(authorProfile.getAvatarUrl())
                                .username(authorProfile.getUsername())
                                .build());
                    } else {
                        // Fallback (Phòng trường hợp lỗi Identity)
                        response.setAuthor(AuthorResponse.builder()
                                .userId(currentUserId)
                                .displayName("Me")
                                .build());
                    }

                    return response;
                })
                .toList();
    }

    public void discardDraft(String id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!recipe.getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Hard Delete (Xóa hẳn khỏi DB)
        recipeRepository.delete(recipe);
    }

    @Transactional
    public RecipePublishResponse publishRecipe(String id, RecipePublishRequest request) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        // 1. Fetch & Authen
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));

        if (!recipe.getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 2. VALIDATION (Chốt chặn kỹ thuật)
        // Phải điền đủ thông tin mới được đăng
        validateMandatoryFields(recipe);

        // 3. MODERATION (Kiểm duyệt nội dung)
        boolean isClean = checkContentSafety(recipe);

        // 4. Quyết định trạng thái
        if (isClean) {
            recipe.setStatus(RecipeStatus.PUBLISHED);
            recipe.setPublishedAt(Instant.now());
            // Trigger Notification cho Follower tại đây
        } else {
            recipe.setStatus(RecipeStatus.PENDING);
            // Trigger Alert cho Admin team tại đây
        }

        // 5. Update Metadata
        recipe.setRecipeVisibility(request.getVisibility());
        recipe.setUpdatedAt(Instant.now());

        // Init stats
        if (recipe.getLikeCount() == 0) recipe.setLikeCount(0);
        var savedRecipe = recipeRepository.save(recipe);

        return RecipePublishResponse.builder()
                .moderationStatus(savedRecipe.getStatus())
                .isPublished(savedRecipe.getStatus() == RecipeStatus.PUBLISHED)
                .build();
    }

    // Hàm Validate bắt buộc nhập
    private void validateMandatoryFields(Recipe recipe) {
        if (!StringUtils.hasText(recipe.getTitle()))
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tiêu đề là bắt buộc");

        if (recipe.getCoverImageUrl() == null || recipe.getCoverImageUrl().isEmpty())
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Cần ít nhất 1 ảnh bìa");

        if (recipe.getFullIngredientList().isEmpty())
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Danh sách nguyên liệu không được trống");

        if (recipe.getSteps().isEmpty())
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Cần ít nhất 1 bước hướng dẫn");
    }

    // Hàm Kiểm duyệt (Demo đơn giản)
    private boolean checkContentSafety(Recipe recipe) {
        // 1. List từ khóa cấm (Nên lưu trong DB hoặc Config)
        List<String> badWords = List.of("bạo lực", "cờ bạc", "lừa đảo", "xxx");

        String contentToCheck = (recipe.getTitle() + " " + recipe.getDescription()).toLowerCase();

        for (String badWord : badWords) {
            if (contentToCheck.contains(badWord)) {
                // Phát hiện từ cấm -> Đánh dấu vi phạm trong ValidationMetadata
                Recipe.ValidationMetadata validation = Recipe.ValidationMetadata.builder()
                        .validationIssues(List.of("Chứa từ khóa nhạy cảm: " + badWord))
                        .validationConfidence(0.9)
                        .build();
                recipe.setValidation(validation);
                return false; // Không an toàn
            }
        }
        return true; // An toàn
    }
}