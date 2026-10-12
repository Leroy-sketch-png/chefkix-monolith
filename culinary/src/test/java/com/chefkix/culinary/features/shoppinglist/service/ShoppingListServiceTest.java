package com.chefkix.culinary.features.shoppinglist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.features.cookplan.entity.CookPlan;
import com.chefkix.culinary.features.cookplan.repository.CookPlanRepository;
import com.chefkix.culinary.features.mealplan.repository.MealPlanRepository;
import com.chefkix.culinary.features.pantry.repository.PantryItemRepository;
import com.chefkix.culinary.features.recipe.repository.RecipeRepository;
import com.chefkix.culinary.features.shoppinglist.dto.request.CreateFromCookPlanRequest;
import com.chefkix.culinary.features.shoppinglist.entity.ShoppingList;
import com.chefkix.culinary.features.shoppinglist.entity.ShoppingListItem;
import com.chefkix.culinary.features.shoppinglist.entity.ShoppingListSource;
import com.chefkix.culinary.features.shoppinglist.repository.ShoppingListRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {

    @Mock private ShoppingListRepository shoppingListRepo;
    @Mock private CookPlanRepository cookPlanRepo;
    @Mock private MealPlanRepository mealPlanRepo;
    @Mock private RecipeRepository recipeRepo;
    @Mock private PantryItemRepository pantryRepo;

    private ShoppingListService service;

    @BeforeEach
    void setUp() {
        service = new ShoppingListService(
                shoppingListRepo, cookPlanRepo, mealPlanRepo, recipeRepo, pantryRepo);
    }

    @Test
    void readingAListPreservesItsStableShareToken() {
        ShoppingList list = ownedList("stable-token");
        when(shoppingListRepo.findByIdAndUserId("list-1", "user-1"))
                .thenReturn(Optional.of(list));

        assertThat(service.getById("user-1", "list-1").getShareToken())
                .isEqualTo("stable-token");
        verify(shoppingListRepo, never()).save(list);
    }

    @Test
    void explicitResetRotatesAndPersistsTheShareToken() {
        ShoppingList list = ownedList("stable-token");
        when(shoppingListRepo.findByIdAndUserId("list-1", "user-1"))
                .thenReturn(Optional.of(list));
        when(shoppingListRepo.save(list)).thenReturn(list);

        service.regenerateShareToken("user-1", "list-1");

        ArgumentCaptor<ShoppingList> saved = ArgumentCaptor.forClass(ShoppingList.class);
        verify(shoppingListRepo).save(saved.capture());
        assertThat(saved.getValue().getShareToken())
                .isNotEqualTo("stable-token")
                .hasSize(12);
    }

    @Test
    void createsAnOwnedCookPlanListWithExactShoppingDataAndProvenance() {
        CreateFromCookPlanRequest request = cookPlanRequest("plan-1");
        CookPlan plan = CookPlan.builder()
                .id("plan-1")
                .userId("user-1")
                .planDate(LocalDate.of(2026, 9, 21))
                .shoppingList(List.of(CookPlan.ShoppingItem.builder()
                        .ingredient("Jasmine rice")
                        .quantity("500")
                        .unit("g")
                        .sourceRecipes(List.of("Family rice bowl"))
                        .build()))
                .build();
        when(shoppingListRepo.findByUserIdAndSourceCookPlanId("user-1", "plan-1"))
                .thenReturn(Optional.empty());
        when(cookPlanRepo.findByIdAndUserId("plan-1", "user-1"))
                .thenReturn(Optional.of(plan));
        when(shoppingListRepo.save(org.mockito.ArgumentMatchers.any(ShoppingList.class)))
                .thenAnswer(invocation -> {
                    ShoppingList saved = invocation.getArgument(0);
                    saved.setId("list-1");
                    return saved;
                });

        var response = service.createFromCookPlan("user-1", request);

        assertThat(response.getSource()).isEqualTo("Cook Plan");
        assertThat(response.getSourceCookPlanId()).isEqualTo("plan-1");
        assertThat(response.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getIngredient()).isEqualTo("Jasmine rice");
            assertThat(item.getQuantity()).isEqualTo("500");
            assertThat(item.getUnit()).isEqualTo("g");
            assertThat(item.getCategory()).isEqualTo("Grains");
            assertThat(item.getRecipes()).containsExactly("Family rice bowl");
        });
    }

    @Test
    void repeatCookPlanSaveReturnsTheExistingListWithoutAnotherWrite() {
        ShoppingList existing = ownedList("stable-token");
        existing.setSource(ShoppingListSource.COOK_PLAN);
        existing.setSourceCookPlanId("plan-1");
        when(shoppingListRepo.findByUserIdAndSourceCookPlanId("user-1", "plan-1"))
                .thenReturn(Optional.of(existing));

        var response = service.createFromCookPlan("user-1", cookPlanRequest("plan-1"));

        assertThat(response.getId()).isEqualTo("list-1");
        verify(shoppingListRepo, never()).save(existing);
        verify(cookPlanRepo, never()).findByIdAndUserId("plan-1", "user-1");
    }

    @Test
    void cookPlanSaveRejectsUnknownOrForeignPlans() {
        when(shoppingListRepo.findByUserIdAndSourceCookPlanId("user-1", "plan-2"))
                .thenReturn(Optional.empty());
        when(cookPlanRepo.findByIdAndUserId("plan-2", "user-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createFromCookPlan("user-1", cookPlanRequest("plan-2")))
                .isInstanceOf(com.chefkix.shared.exception.AppException.class);
        verify(shoppingListRepo, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void copiesSharedListIntoIndependentRecipientOwnedWork() {
        ShoppingList source = ownedList("shared-token");
        source.setUserId("sender-1");
        source.setSource(ShoppingListSource.MEAL_PLAN);
        source.setSourceMealPlanId("sender-plan");
        source.setItems(new ArrayList<>(List.of(ShoppingListItem.builder()
                .itemId("source-item")
                .ingredient("Jasmine rice")
                .quantity("500")
                .unit("g")
                .category("Grains")
                .recipes(new ArrayList<>(List.of("Family rice bowl")))
                .checked(true)
                .addedManually(false)
                .build())));
        when(shoppingListRepo.findByShareToken("shared-token"))
                .thenReturn(Optional.of(source));
        when(shoppingListRepo.save(org.mockito.ArgumentMatchers.any(ShoppingList.class)))
                .thenAnswer(invocation -> {
                    ShoppingList saved = invocation.getArgument(0);
                    saved.setId("recipient-list");
                    return saved;
                });

        var response = service.copySharedList("recipient-1", "shared-token");

        ArgumentCaptor<ShoppingList> saved = ArgumentCaptor.forClass(ShoppingList.class);
        verify(shoppingListRepo).save(saved.capture());
        ShoppingList copy = saved.getValue();
        assertThat(copy.getUserId()).isEqualTo("recipient-1");
        assertThat(copy.getName()).isEqualTo(source.getName());
        assertThat(copy.getSource()).isEqualTo(ShoppingListSource.CUSTOM);
        assertThat(copy.getSourceMealPlanId()).isNull();
        assertThat(copy.getShareToken()).isNotEqualTo(source.getShareToken()).hasSize(12);
        assertThat(response.getId()).isEqualTo("recipient-list");
        assertThat(copy.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getItemId()).isNotEqualTo("source-item");
            assertThat(item.getIngredient()).isEqualTo("Jasmine rice");
            assertThat(item.isChecked()).isFalse();
            assertThat(item.getRecipes()).containsExactly("Family rice bowl");
            assertThat(item.getRecipes()).isNotSameAs(source.getItems().get(0).getRecipes());
        });
        assertThat(source.getItems().get(0).isChecked()).isTrue();
    }

    @Test
    void copySharedListRejectsAnUnknownShareToken() {
        when(shoppingListRepo.findByShareToken("missing-token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.copySharedList("recipient-1", "missing-token"))
                .isInstanceOf(com.chefkix.shared.exception.AppException.class);
        verify(shoppingListRepo, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private CreateFromCookPlanRequest cookPlanRequest(String planId) {
        CreateFromCookPlanRequest request = new CreateFromCookPlanRequest();
        request.setCookPlanId(planId);
        return request;
    }

    private ShoppingList ownedList(String shareToken) {
        return ShoppingList.builder()
                .id("list-1")
                .userId("user-1")
                .name("Friday groceries")
                .items(new ArrayList<>())
                .source(ShoppingListSource.CUSTOM)
                .shareToken(shareToken)
                .build();
    }
}
