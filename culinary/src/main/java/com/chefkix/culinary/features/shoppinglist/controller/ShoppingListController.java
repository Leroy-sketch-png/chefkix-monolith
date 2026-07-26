package com.chefkix.culinary.features.shoppinglist.controller;

import com.chefkix.culinary.features.shoppinglist.dto.request.AddCustomItemRequest;
import com.chefkix.culinary.features.shoppinglist.dto.request.CreateCustomListRequest;
import com.chefkix.culinary.features.shoppinglist.dto.request.CreateFromMealPlanRequest;
import com.chefkix.culinary.features.shoppinglist.dto.request.CreateFromRecipeRequest;
import com.chefkix.culinary.features.shoppinglist.dto.response.ShoppingListResponse;
import com.chefkix.culinary.features.shoppinglist.dto.response.ShoppingListSummaryResponse;
import com.chefkix.culinary.features.shoppinglist.entity.CheckoutRecord;
import com.chefkix.culinary.features.shoppinglist.grocery.GroceryProvider;
import com.chefkix.culinary.features.shoppinglist.grocery.GroceryProviderRegistry;
import com.chefkix.culinary.features.shoppinglist.repository.CheckoutRecordRepository;
import com.chefkix.culinary.features.shoppinglist.service.ShoppingListService;
import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shopping-lists")
@RequiredArgsConstructor
public class ShoppingListController {

    private final ShoppingListService shoppingListService;
    private final GroceryProviderRegistry groceryProviderRegistry;
    private final CheckoutRecordRepository checkoutRecordRepository;


    @PostMapping("/from-meal-plan")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShoppingListResponse> createFromMealPlan(
            @Valid @RequestBody CreateFromMealPlanRequest request) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(201)
                .data(shoppingListService.createFromMealPlan(userId(), request))
                .build();
    }

    @PostMapping("/from-recipe")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShoppingListResponse> createFromRecipe(
            @Valid @RequestBody CreateFromRecipeRequest request) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(201)
                .data(shoppingListService.createFromRecipe(userId(), request))
                .build();
    }

    @PostMapping("/custom")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShoppingListResponse> createCustom(
            @Valid @RequestBody CreateCustomListRequest request) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(201)
                .data(shoppingListService.createCustom(userId(), request))
                .build();
    }


    @GetMapping
    public ApiResponse<List<ShoppingListSummaryResponse>> getUserLists() {
        return ApiResponse.<List<ShoppingListSummaryResponse>>builder()
                .success(true).statusCode(200)
                .data(shoppingListService.getUserLists(userId()))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ShoppingListResponse> getById(@PathVariable String id) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(200)
                .data(shoppingListService.getById(userId(), id))
                .build();
    }

    @GetMapping("/shared/{shareToken}")
    public ApiResponse<ShoppingListResponse> getByShareToken(@PathVariable String shareToken) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(200)
                .data(shoppingListService.getByShareToken(shareToken))
                .build();
    }


    @PutMapping("/{id}/items/{itemId}/toggle")
    public ApiResponse<ShoppingListResponse> toggleItem(
            @PathVariable String id, @PathVariable String itemId) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(200)
                .data(shoppingListService.toggleItem(userId(), id, itemId))
                .build();
    }

    @PostMapping("/{id}/items")
    public ApiResponse<ShoppingListResponse> addCustomItem(
            @PathVariable String id, @Valid @RequestBody AddCustomItemRequest request) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(200)
                .data(shoppingListService.addCustomItem(userId(), id, request))
                .build();
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ApiResponse<ShoppingListResponse> removeItem(
            @PathVariable String id, @PathVariable String itemId) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(200)
                .data(shoppingListService.removeItem(userId(), id, itemId))
                .build();
    }


    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        shoppingListService.delete(userId(), id);
        return ApiResponse.<Void>builder()
                .success(true).statusCode(200)
                .build();
    }


    @PostMapping("/{id}/share")
    public ApiResponse<ShoppingListResponse> regenerateShareToken(@PathVariable String id) {
        return ApiResponse.<ShoppingListResponse>builder()
                .success(true).statusCode(200)
                .data(shoppingListService.regenerateShareToken(userId(), id))
                .build();
    }


    /**
     */
    @PostMapping("/{id}/checkout")
    public ApiResponse<GroceryProvider.CheckoutResult> checkout(
            @PathVariable String id,
            @RequestParam(defaultValue = "manual") String provider) {
        GroceryProvider groceryProvider = requireAvailableProvider(provider);
        ShoppingListResponse list = shoppingListService.getById(userId(), id);
        List<GroceryProvider.GroceryItemRequest> items = list.getItems().stream()
                .filter(item -> !item.isChecked())
                .map(item -> new GroceryProvider.GroceryItemRequest(
                        item.getItemId(), item.getIngredient(),
                        item.getQuantity(), item.getUnit(), item.getCategory()))
                .toList();

        GroceryProvider.CheckoutResult result = groceryProvider.createCheckout(items, userId());

        checkoutRecordRepository.save(CheckoutRecord.builder()
                .orderId(result.orderId())
                .userId(userId())
                .shoppingListId(id)
                .provider(result.provider())
                .itemCount(result.itemCount())
                .estimatedTotal(result.estimatedTotal())
                .checkoutUrl(result.checkoutUrl())
                .status("redirected")
                .build());

        return ApiResponse.<GroceryProvider.CheckoutResult>builder()
                .success(true).statusCode(200).data(result).build();
    }

    /**
     */
    @GetMapping("/checkout-status/{orderId}")
    public ApiResponse<GroceryProvider.OrderStatus> checkoutStatus(
            @PathVariable String orderId,
            @RequestParam(defaultValue = "manual") String provider) {
        GroceryProvider groceryProvider = requireAvailableProvider(provider);
        GroceryProvider.OrderStatus status = groceryProvider.getOrderStatus(orderId);

        checkoutRecordRepository.findByOrderId(orderId).ifPresent(record -> {
            if (!record.getStatus().equals(status.status())) {
                record.setStatus(status.status());
                checkoutRecordRepository.save(record);
            }
        });

        return ApiResponse.<GroceryProvider.OrderStatus>builder()
                .success(true).statusCode(200).data(status).build();
    }

    /**
     */
    @GetMapping("/grocery-providers")
    public ApiResponse<List<GroceryProviderRegistry.ProviderInfo>> getProviders() {
        return ApiResponse.<List<GroceryProviderRegistry.ProviderInfo>>builder()
                .success(true).statusCode(200)
                .data(groceryProviderRegistry.getAvailableProviders()).build();
    }

    /**
     */
    @PostMapping("/ingredient-links")
    public ApiResponse<Map<String, String>> getIngredientLinks(
            @Valid @RequestBody List<GroceryProvider.GroceryItemRequest> items,
            @RequestParam(defaultValue = "affiliate") String provider) {
        GroceryProvider groceryProvider = groceryProviderRegistry.getProvider(provider);
        if (groceryProvider == null || !groceryProvider.isAvailable()) {
            return ApiResponse.<Map<String, String>>builder()
                    .success(true).statusCode(200).data(Map.of()).build();
        }
        Map<String, String> links = groceryProvider.getPerIngredientLinks(items);
        return ApiResponse.<Map<String, String>>builder()
                .success(true).statusCode(200).data(links).build();
    }


    private String userId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private GroceryProvider requireAvailableProvider(String providerId) {
        GroceryProvider provider = groceryProviderRegistry.getProvider(providerId);
        if (provider == null) {
            throw new AppException(
                    ErrorCode.INVALID_REQUEST,
                    "Grocery checkout is unavailable for the requested provider");
        }
        return provider;
    }
}
