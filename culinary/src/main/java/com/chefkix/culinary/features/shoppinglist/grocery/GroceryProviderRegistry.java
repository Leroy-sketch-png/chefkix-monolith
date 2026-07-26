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
 *
 *
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
     */
    public GroceryProvider getProvider(String providerId) {
        if (providerId == null) return defaultProvider;
        GroceryProvider provider = providers.get(providerId);
        if (provider == null || !provider.isAvailable()) {
            log.warn("Provider '{}' not available", providerId);
            return null;
        }
        return provider;
    }

    /**
     */
    public List<ProviderInfo> getAvailableProviders() {
        return providers.values().stream()
                .filter(GroceryProvider::isAvailable)
                .map(p -> new ProviderInfo(p.getProviderId(), p.getDisplayName()))
                .toList();
    }

    public record ProviderInfo(String id, String displayName) {}
}
