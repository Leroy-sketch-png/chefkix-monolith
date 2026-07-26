package com.chefkix.culinary.features.shoppinglist.grocery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroceryProviderRegistryTest {

    private GroceryProvider manualProvider;
    private GroceryProvider affiliateProvider;
    private GroceryProviderRegistry registry;

    @BeforeEach
    void setUp() {
        manualProvider = mock(GroceryProvider.class);
        affiliateProvider = mock(GroceryProvider.class);

        when(manualProvider.getProviderId()).thenReturn("manual");
        when(manualProvider.isAvailable()).thenReturn(true);
        when(affiliateProvider.getProviderId()).thenReturn("affiliate");
        when(affiliateProvider.isAvailable()).thenReturn(false);

        registry = new GroceryProviderRegistry(List.of(manualProvider, affiliateProvider));
    }

    @Test
    void nullProviderUsesManualDefault() {
        assertThat(registry.getProvider(null)).isSameAs(manualProvider);
    }

    @Test
    void explicitAvailableProviderIsReturned() {
        assertThat(registry.getProvider("manual")).isSameAs(manualProvider);
    }

    @Test
    void explicitUnavailableProviderDoesNotFallBack() {
        assertThat(registry.getProvider("affiliate")).isNull();
    }

    @Test
    void explicitUnknownProviderDoesNotFallBack() {
        assertThat(registry.getProvider("unknown")).isNull();
    }
}
