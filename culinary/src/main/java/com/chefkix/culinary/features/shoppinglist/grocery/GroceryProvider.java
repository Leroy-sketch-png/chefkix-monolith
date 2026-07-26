package com.chefkix.culinary.features.shoppinglist.grocery;

import java.util.List;
import java.util.Map;

/**
 * 
 */
public interface GroceryProvider {

    /**
     */
    String getProviderId();

    /**
     */
    String getDisplayName();

    /**
     */
    boolean isAvailable();

    /**
     */
    List<GroceryProductMatch> matchProducts(List<GroceryItemRequest> items);

    /**
     */
    CheckoutResult createCheckout(List<GroceryItemRequest> items, String userId);

    /**
     */
    OrderStatus getOrderStatus(String orderId);

    /**
     */
    default Map<String, String> getPerIngredientLinks(List<GroceryItemRequest> items) {
return Map.of();
    }


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
String status,
            String estimatedDelivery,
            String trackingUrl
    ) {}
}
