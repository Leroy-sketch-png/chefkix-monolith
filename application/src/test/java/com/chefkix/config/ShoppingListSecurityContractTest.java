package com.chefkix.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class ShoppingListSecurityContractTest {

    @Test
    void sharedListsArePublicForReadsButNotForMutations() throws Exception {
        assertThat(endpointArray("PUBLIC_ENDPOINTS"))
                .doesNotContain("/shopping-lists/shared/**");
        assertThat(endpointArray("GUEST_GET_ENDPOINTS"))
                .contains("/shopping-lists/shared/**");
    }

    private String[] endpointArray(String fieldName) throws Exception {
        Field field = SecurityConfig.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String[]) field.get(null);
    }
}
