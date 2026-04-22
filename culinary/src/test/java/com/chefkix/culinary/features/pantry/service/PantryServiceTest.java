package com.chefkix.culinary.features.pantry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.features.pantry.dto.request.PantryItemRequest;
import com.chefkix.culinary.features.pantry.dto.response.PantryItemResponse;
import com.chefkix.culinary.features.pantry.entity.PantryItem;
import com.chefkix.culinary.features.pantry.repository.PantryItemRepository;
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
class PantryServiceTest {

  @Mock
  private PantryItemRepository pantryRepo;

  @Mock
  private RecipeRepository recipeRepo;

  private PantryService pantryService;

  @BeforeEach
  void setUp() {
    pantryService = new FixedTodayPantryService(
        pantryRepo,
        recipeRepo,
        LocalDate.of(2026, 4, 22));
  }

  @Test
  void addItemUsesUtcTodayForAddedDate() {
    PantryItemRequest request = PantryItemRequest.builder()
        .ingredientName("Milk")
        .quantity(1.0)
        .unit("L")
        .category("Dairy")
        .build();

    when(pantryRepo.findByUserIdAndNormalizedName("user-1", "milk"))
        .thenReturn(Optional.empty());
    when(pantryRepo.save(org.mockito.ArgumentMatchers.any(PantryItem.class)))
        .thenAnswer(invocation -> {
          PantryItem saved = invocation.getArgument(0);
          saved.setId("item-1");
          return saved;
        });

    PantryItemResponse response = pantryService.addItem("user-1", request);

    ArgumentCaptor<PantryItem> itemCaptor = ArgumentCaptor.forClass(PantryItem.class);
    verify(pantryRepo).save(itemCaptor.capture());
    assertThat(itemCaptor.getValue().getAddedDate()).isEqualTo(LocalDate.of(2026, 4, 22));
    assertThat(response.getAddedDate()).isEqualTo(LocalDate.of(2026, 4, 22));
  }

  @Test
  void clearExpiredUsesUtcTodayForExpiryBoundary() {
    when(pantryRepo.findByUserIdAndExpiryDateBefore("user-1", LocalDate.of(2026, 4, 22)))
        .thenReturn(List.of(PantryItem.builder().id("item-1").build()));

    long cleared = pantryService.clearExpired("user-1");

    assertThat(cleared).isEqualTo(1);
    verify(pantryRepo).findByUserIdAndExpiryDateBefore("user-1", LocalDate.of(2026, 4, 22));
  }

  @Test
  void getAllComputesFreshnessRelativeToUtcToday() {
    PantryItem expiringSoon = PantryItem.builder()
        .id("item-1")
        .userId("user-1")
        .ingredientName("Spinach")
        .normalizedName("spinach")
        .category("produce")
        .expiryDate(LocalDate.of(2026, 4, 24))
        .addedDate(LocalDate.of(2026, 4, 20))
        .build();

    when(pantryRepo.findByUserId(eq("user-1"), org.mockito.ArgumentMatchers.any(Sort.class)))
        .thenReturn(List.of(expiringSoon));

    List<PantryItemResponse> responses = pantryService.getAll("user-1", null, "category");

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).getFreshness()).isEqualTo("expiring_soon");
  }

  private static final class FixedTodayPantryService extends PantryService {
    private final LocalDate today;

    private FixedTodayPantryService(
        PantryItemRepository pantryRepo,
        RecipeRepository recipeRepo,
        LocalDate today) {
      super(pantryRepo, recipeRepo);
      this.today = today;
    }

    @Override
    protected LocalDate utcToday() {
      return today;
    }
  }
}