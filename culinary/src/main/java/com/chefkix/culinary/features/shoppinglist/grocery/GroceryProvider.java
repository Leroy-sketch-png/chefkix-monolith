package com.chefkix.culinary.features.shoppinglist.grocery;

import java.util.List;
import java.util.Map;

/**
 * Provider-agnostic grocery delivery interface.
 * Implementations serve different grocery providers (Instacart, DoorDash, manual, etc.)
 * 
 * Per spec 26-grocery-delivery.txt:
 * Phase 1: Instacart Connect API
 * Phase 2: Multi-provider support
 */
public interface GroceryProvider {

    /**
     * Get the provider identifier.
     */
    String getProviderId();

    /**
     * Get the provider display name.
     */
    String getDisplayName();

    /**
     * Check if this provider is available/configured.
     */
    boolean isAvailable();

    /**
     * Match shopping list items to provider products.
     * Returns a map of itemId → matched product info.
     */
    List<GroceryProductMatch> matchProducts(List<GroceryItemRequest> items);

    /**
     * Create a cart and return a checkout URL for the provider.
     */
    CheckoutResult createCheckout(List<GroceryItemRequest> items, String userId);

    /**
     * Get the status of an existing order.
     */
    OrderStatus getOrderStatus(String orderId);

    // ─── DTOs ───────────────────────────────────────────────────────

    record GroceryItemRequest(
            String itemId,
            String name,
            String quantity,
            String unit,
            String category
    ) {}

    record GroceryProductMatch(
            String itemId,
            String productId,
            String productName,
            String imageUrl,
            double price,
            String unit,
            double confidence
    ) {}

    record CheckoutResult(
            String orderId,
            String checkoutUrl,
            String provider,
            int itemCount,
            double estimatedTotal,
            String status
    ) {}

    record OrderStatus(
            String orderId,
            String status,  // "pending", "confirmed", "in_progress", "delivered", "cancelled"
            String estimatedDelivery,
            String trackingUrl
    ) {}
}
