package com.chefkix.culinary.features.mealplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.common.client.AIRestClient;
import com.chefkix.culinary.features.ai.dto.internal.AIMealPlanResponse;
import com.chefkix.culinary.features.mealplan.dto.request.GenerateMealPlanRequest;
import com.chefkix.culinary.features.mealplan.dto.response.MealPlanResponse;
import com.chefkix.culinary.features.mealplan.entity.MealPlan;
import com.chefkix.culinary.features.mealplan.repository.MealPlanRepository;
import com.chefkix.culinary.features.pantry.repository.PantryItemRepository;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class MealPlanServiceTest {

  @Mock
  private MealPlanRepository mealPlanRepo;

  @Mock
  private RecipeRepository recipeRepo;

  @Mock
  private PantryItemRepository pantryRepo;

  @Mock
  private AIRestClient aiRestClient;

  private MealPlanService mealPlanService;

  @BeforeEach
  void setUp() {
    mealPlanService = new FixedTodayMealPlanService(
        mealPlanRepo,
        recipeRepo,
        pantryRepo,
        aiRestClient,
        LocalDate.of(2026, 4, 22));
  }

  @Test
  void getCurrentUsesUtcWeekStartDate() {
    MealPlan existingPlan = MealPlan.builder()
        .id("plan-1")
        .userId("user-1")
        .weekStartDate(LocalDate.of(2026, 4, 20))
        .build();
    when(mealPlanRepo.findByUserIdAndWeekStartDate("user-1", LocalDate.of(2026, 4, 20)))
        .thenReturn(Optional.of(existingPlan));

    MealPlanResponse response = mealPlanService.getCurrent("user-1");

    assertThat(response).isNotNull();
    assertThat(response.getWeekStartDate()).isEqualTo(LocalDate.of(2026, 4, 20));
    verify(mealPlanRepo).findByUserIdAndWeekStartDate("user-1", LocalDate.of(2026, 4, 20));
  }

  @Test
  void generateRuleBasedUsesUtcWeekStartDateWhenSaving() {
    GenerateMealPlanRequest request = GenerateMealPlanRequest.builder()
        .days(1)
        .pantryItems(List.of("rice"))
        .build();
    Recipe recipe = Recipe.builder()
        .id("recipe-1")
        .title("Rice Bowl")
        .totalTimeMinutes(15)
        .servings(2)
        .build();

    when(recipeRepo.findPublishedForIngredientMatching()).thenReturn(List.of(recipe));
    when(mealPlanRepo.save(any(MealPlan.class))).thenAnswer(invocation -> {
      MealPlan plan = invocation.getArgument(0);
      plan.setId("plan-1");
      return plan;
    });

    MealPlanResponse response = mealPlanService.generate("user-1", request, false);

    ArgumentCaptor<MealPlan> planCaptor = ArgumentCaptor.forClass(MealPlan.class);
    verify(mealPlanRepo).save(planCaptor.capture());
    assertThat(planCaptor.getValue().getWeekStartDate()).isEqualTo(LocalDate.of(2026, 4, 20));
    assertThat(response.getWeekStartDate()).isEqualTo(LocalDate.of(2026, 4, 20));
  }

  @Test
  void generateWithAiUsesUtcWeekStartDateWhenSaving() {
    GenerateMealPlanRequest request = GenerateMealPlanRequest.builder()
        .days(2)
        .pantryItems(List.of("tomato", "pasta"))
        .build();
    AIMealPlanResponse aiResponse = AIMealPlanResponse.builder()
        .mealPlan(List.of(AIMealPlanResponse.AIDayPlan.builder().day(1).build()))
        .shoppingList(List.of("basil"))
        .reasoning("Use the pantry first")
        .pantryUtilizationPercent(75.0)
        .build();

    when(aiRestClient.generateMealPlan(any())).thenReturn(aiResponse);
    when(mealPlanRepo.save(any(MealPlan.class))).thenAnswer(invocation -> {
      MealPlan plan = invocation.getArgument(0);
      plan.setId("plan-2");
      return plan;
    });

    MealPlanResponse response = mealPlanService.generate("user-1", request, true);

    ArgumentCaptor<MealPlan> planCaptor = ArgumentCaptor.forClass(MealPlan.class);
    verify(mealPlanRepo).save(planCaptor.capture());
    assertThat(planCaptor.getValue().getWeekStartDate()).isEqualTo(LocalDate.of(2026, 4, 20));
    assertThat(response.getWeekStartDate()).isEqualTo(LocalDate.of(2026, 4, 20));
    assertThat(response.getReasoning()).isEqualTo("Use the pantry first");
  }

  private static final class FixedTodayMealPlanService extends MealPlanService {
    private final LocalDate today;

    private FixedTodayMealPlanService(
        MealPlanRepository mealPlanRepo,
        RecipeRepository recipeRepo,
        PantryItemRepository pantryRepo,
        AIRestClient aiRestClient,
        LocalDate today) {
      super(mealPlanRepo, recipeRepo, pantryRepo, aiRestClient);
      this.today = today;
    }

    @Override
    protected LocalDate utcToday() {
      return today;
    }
  }
}