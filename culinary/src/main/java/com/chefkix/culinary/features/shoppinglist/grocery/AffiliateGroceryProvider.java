package com.chefkix.culinary.features.shoppinglist.grocery;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Affiliate grocery provider — generates partner checkout URLs with affiliate tracking.
 * Revenue model: shopping list → affiliate checkout URL → grocery delivery partner → commission.
 *
 * Phase 0: Builds parameterized affiliate links. Actual partner API integration in Phase 1.
 * Configurable via application.yml:
 *   chefkix.grocery.affiliate.enabled=true
 *   chefkix.grocery.affiliate.partner-tag=chefkix-app
 *   chefkix.grocery.affiliate.base-url=https://www.instacart.com
 */
@Slf4j
@Service
public class AffiliateGroceryProvider implements GroceryProvider {

  @Value("${chefkix.grocery.affiliate.enabled:false}")
  private boolean enabled;

  @Value("${chefkix.grocery.affiliate.partner-tag:chefkix-app}")
  private String partnerTag;

  @Value("${chefkix.grocery.affiliate.base-url:https://www.instacart.com}")
  private String baseUrl;

  @Override
  public String getProviderId() {
    return "affiliate";
  }

  @Override
  public String getDisplayName() {
    return "Order Ingredients";
  }

  @Override
  public boolean isAvailable() {
    return enabled;
  }

  @Override
  public List<GroceryProductMatch> matchProducts(List<GroceryItemRequest> items) {
    // Phase 0: identity match with affiliate links — no real product lookup yet
    return items.stream()
        .map(
            item ->
                new GroceryProductMatch(
                    item.itemId(),
                    "aff-" + item.itemId(),
                    item.name(),
                    null,
                    0.0,
                    item.unit(),
                    0.8))
        .toList();
  }

  @Override
  public CheckoutResult createCheckout(List<GroceryItemRequest> items, String userId) {
    String orderId = "aff-" + UUID.randomUUID().toString().substring(0, 8);

    // Build affiliate checkout URL with ingredients as search params
    String ingredientList =
        items.stream()
            .map(GroceryItemRequest::name)
            .collect(Collectors.joining(","));

    String checkoutUrl =
        baseUrl
            + "/store/search?q="
            + URLEncoder.encode(ingredientList, StandardCharsets.UTF_8)
            + "&utm_source="
            + URLEncoder.encode(partnerTag, StandardCharsets.UTF_8)
            + "&ref="
            + URLEncoder.encode(partnerTag, StandardCharsets.UTF_8);

    log.info(
        "Affiliate checkout created: orderId={}, userId={}, itemCount={}, partner={}",
        orderId,
        userId,
        items.size(),
        partnerTag);

    return new CheckoutResult(orderId, checkoutUrl, "affiliate", items.size(), 0.0, "redirecting");
  }

  @Override
  public OrderStatus getOrderStatus(String orderId) {
    // Affiliate orders are tracked externally — we only have the redirect
    return new OrderStatus(orderId, "redirected", null, null);
  }

  @Override
  public Map<String, String> getPerIngredientLinks(List<GroceryItemRequest> items) {
    if (!enabled) return Map.of();

    Map<String, String> links = new HashMap<>();
    for (GroceryItemRequest item : items) {
      String url = baseUrl
          + "/store/search?q="
          + URLEncoder.encode(item.name(), StandardCharsets.UTF_8)
          + "&utm_source="
          + URLEncoder.encode(partnerTag, StandardCharsets.UTF_8)
          + "&ref="
          + URLEncoder.encode(partnerTag, StandardCharsets.UTF_8);
      links.put(item.itemId() != null ? item.itemId() : item.name(), url);
    }
    return links;
  }
}
