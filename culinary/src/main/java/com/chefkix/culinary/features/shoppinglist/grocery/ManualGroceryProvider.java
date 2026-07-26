package com.chefkix.culinary.features.shoppinglist.grocery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 *
 */
@Slf4j
@Service
public class ManualGroceryProvider implements GroceryProvider {

    @Override
    public String getProviderId() {
        return "manual";
    }

    @Override
    public String getDisplayName() {
        return "Manual Shopping";
    }

    @Override
    public boolean isAvailable() {
return true;
    }

    @Override
    public List<GroceryProductMatch> matchProducts(List<GroceryItemRequest> items) {
        return items.stream()
                .map(item -> new GroceryProductMatch(
                        item.itemId(),
                        "manual-" + item.itemId(),
                        item.name(),
                        null,
                        0.0,
                        item.unit(),
                        1.0
                ))
                .toList();
    }

    @Override
    public CheckoutResult createCheckout(List<GroceryItemRequest> items, String userId) {
        String orderId = "manual-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Manual checkout created for user {} with {} items", userId, items.size());

        return new CheckoutResult(
                orderId,
null,
                "manual",
                items.size(),
                0.0,
                "confirmed"
        );
    }

    @Override
    public OrderStatus getOrderStatus(String orderId) {
        return new OrderStatus(
                orderId,
                "confirmed",
                null,
                null
        );
    }
}
