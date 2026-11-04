package com.chefkix.culinary.features.shoppinglist.grocery;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AffiliateGroceryProviderTest {

  @Test
  @DisplayName("Default disabled provider reports unavailable and returns empty ingredient links")
  void disabledProviderReportsUnavailableAndReturnsEmptyLinks() {
    AffiliateGroceryProvider provider =
        new AffiliateGroceryProvider(false, "chefkix-app", "https://www.instacart.com");

    assertThat(provider.getProviderId()).isEqualTo("affiliate");
    assertThat(provider.getDisplayName()).isEqualTo("Order Ingredients");
    assertThat(provider.isAvailable()).isFalse();

    List<GroceryProvider.GroceryItemRequest> items =
        List.of(
            new GroceryProvider.GroceryItemRequest(
                "item-1", "Organic Milk", "1", "gallon", "Dairy"),
            new GroceryProvider.GroceryItemRequest(
                "item-2", "Eggs", "12", "count", "Dairy"));

    Map<String, String> links = provider.getPerIngredientLinks(items);
    assertThat(links).isEmpty();
  }

  @Test
  @DisplayName("Enabled provider reports available and generates UTM-tagged search checkout URL")
  void enabledProviderCreatesOutboundSearchCheckoutUrl() {
    AffiliateGroceryProvider provider =
        new AffiliateGroceryProvider(true, "chefkix-tag", "https://www.instacart.com");

    assertThat(provider.isAvailable()).isTrue();

    List<GroceryProvider.GroceryItemRequest> items =
        List.of(
            new GroceryProvider.GroceryItemRequest("item-1", "Garlic", "3", "cloves", "Produce"),
            new GroceryProvider.GroceryItemRequest(
                "item-2", "Olive Oil", "2", "tbsp", "Pantry"));

    GroceryProvider.CheckoutResult checkout = provider.createCheckout(items, "user-42");

    assertThat(checkout.orderId()).startsWith("aff-");
    assertThat(checkout.provider()).isEqualTo("affiliate");
    assertThat(checkout.itemCount()).isEqualTo(2);
    assertThat(checkout.status()).isEqualTo("redirecting");
    assertThat(checkout.checkoutUrl())
        .startsWith("https://www.instacart.com/store/search?q=")
        .contains("utm_source=chefkix-tag")
        .contains("ref=chefkix-tag")
        .contains("Garlic%2COlive+Oil");
  }

  @Test
  @DisplayName("Enabled provider constructs per-ingredient outbound search links")
  void enabledProviderBuildsPerIngredientSearchLinks() {
    AffiliateGroceryProvider provider =
        new AffiliateGroceryProvider(true, "chefkix-partner", "https://delivery.example.com");

    List<GroceryProvider.GroceryItemRequest> items =
        List.of(
            new GroceryProvider.GroceryItemRequest(
                "item-alpha", "Fresh Basil", "1", "bunch", "Produce"),
            new GroceryProvider.GroceryItemRequest(
                null, "Black Pepper", "1", "tsp", "Spice"));

    Map<String, String> links = provider.getPerIngredientLinks(items);

    assertThat(links).hasSize(2);
    assertThat(links).containsKey("item-alpha");
    assertThat(links.get("item-alpha"))
        .startsWith("https://delivery.example.com/store/search?q=Fresh+Basil")
        .contains("utm_source=chefkix-partner")
        .contains("ref=chefkix-partner");

    // When itemId is null, fallback key is item.name()
    assertThat(links).containsKey("Black Pepper");
    assertThat(links.get("Black Pepper"))
        .startsWith("https://delivery.example.com/store/search?q=Black+Pepper")
        .contains("utm_source=chefkix-partner");
  }

  @Test
  @DisplayName("Order status truthfully reports redirected without remote carrier tracking")
  void orderStatusTruthfullyReportsRedirected() {
    AffiliateGroceryProvider provider =
        new AffiliateGroceryProvider(true, "chefkix-tag", "https://www.instacart.com");

    GroceryProvider.OrderStatus status = provider.getOrderStatus("aff-abc12345");

    assertThat(status.orderId()).isEqualTo("aff-abc12345");
    assertThat(status.status()).isEqualTo("redirected");
    assertThat(status.trackingUrl()).isNull();
    assertThat(status.estimatedDelivery()).isNull();
  }

  @Test
  @DisplayName("Product matching maps items with provisional aff- identifiers")
  void productMatchingMapsItemsWithProvisionalIdentifiers() {
    AffiliateGroceryProvider provider =
        new AffiliateGroceryProvider(true, "chefkix-tag", "https://www.instacart.com");

    List<GroceryProvider.GroceryItemRequest> items =
        List.of(
            new GroceryProvider.GroceryItemRequest("item-9", "Tomato", "4", "whole", "Produce"));

    List<GroceryProvider.GroceryProductMatch> matches = provider.matchProducts(items);

    assertThat(matches).hasSize(1);
    GroceryProvider.GroceryProductMatch match = matches.get(0);
    assertThat(match.itemId()).isEqualTo("item-9");
    assertThat(match.productId()).isEqualTo("aff-item-9");
    assertThat(match.productName()).isEqualTo("Tomato");
    assertThat(match.unit()).isEqualTo("whole");
    assertThat(match.confidence()).isEqualTo(0.8);
  }
}
