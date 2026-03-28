package com.chefkix.culinary.features.shoppinglist.grocery;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages grocery delivery providers and routes requests to the right one.
 * Per spec 26-grocery-delivery.txt: multi-provider support.
 *
 * Currently supports:
 * - "manual" (default) — printable shopping list, no external delivery
 *
 * Future:
 * - "instacart" — Instacart Connect API
 * - "doordash" — DoorDash Drive API
 */
@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroceryProviderRegistry {

    Map<String, GroceryProvider> providers;
    GroceryProvider defaultProvider;

    public GroceryProviderRegistry(List<GroceryProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(GroceryProvider::getProviderId, Function.identity()));
        this.defaultProvider = providers.get("manual");

        log.info("Registered {} grocery providers: {}", providers.size(), providers.keySet());
    }

    /**
     * Get a provider by ID, falling back to default.
     */
    public GroceryProvider getProvider(String providerId) {
        if (providerId == null) return defaultProvider;
        GroceryProvider provider = providers.get(providerId);
        if (provider == null || !provider.isAvailable()) {
            log.warn("Provider '{}' not available, using default", providerId);
            return defaultProvider;
        }
        return provider;
    }

    /**
     * List all available providers for the FE provider picker.
     */
    public List<ProviderInfo> getAvailableProviders() {
        return providers.values().stream()
                .filter(GroceryProvider::isAvailable)
                .map(p -> new ProviderInfo(p.getProviderId(), p.getDisplayName()))
                .toList();
    }

    public record ProviderInfo(String id, String displayName) {}
}
