package com.chefkix.culinary.features.shoppinglist.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.features.shoppinglist.grocery.GroceryProviderRegistry;
import com.chefkix.culinary.features.shoppinglist.repository.CheckoutRecordRepository;
import com.chefkix.culinary.features.shoppinglist.service.ShoppingListService;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShoppingListControllerTest {

    @Mock
    private ShoppingListService shoppingListService;

    @Mock
    private GroceryProviderRegistry groceryProviderRegistry;

    @Mock
    private CheckoutRecordRepository checkoutRecordRepository;

    private ShoppingListController controller;

    @BeforeEach
    void setUp() {
        controller = new ShoppingListController(
                shoppingListService,
                groceryProviderRegistry,
                checkoutRecordRepository);
    }

    @Test
    void checkoutRejectsUnavailableProviderBeforeListOrPersistenceAccess() {
        when(groceryProviderRegistry.getProvider("affiliate")).thenReturn(null);

        assertThatThrownBy(() -> controller.checkout("list-1", "affiliate"))
                .isInstanceOfSatisfying(AppException.class, exception -> {
                    org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_REQUEST);
                    org.assertj.core.api.Assertions.assertThat(exception.getMessage())
                            .isEqualTo("Grocery checkout is unavailable for the requested provider");
                });

        verifyNoInteractions(shoppingListService, checkoutRecordRepository);
    }

    @Test
    void checkoutStatusRejectsUnknownProviderBeforeRepositoryAccess() {
        when(groceryProviderRegistry.getProvider("unknown")).thenReturn(null);

        assertThatThrownBy(() -> controller.checkoutStatus("order-1", "unknown"))
                .isInstanceOf(AppException.class);

        verifyNoInteractions(shoppingListService, checkoutRecordRepository);
    }
}
