package com.chefkix.culinary.common.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.common.dto.response.AuthorResponse;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.shared.util.UploadImageFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncHelperTest {

    @Mock ProfileProvider profileProvider;
    @Mock UploadImageFile uploadImageFile;

    @Test
    void preservesServerOwnedVerificationWhenBuildingRecipeAuthor() throws Exception {
        when(profileProvider.getBasicProfile("creator-1")).thenReturn(BasicProfileInfo.builder()
                .userId("creator-1")
                .username("minh")
                .displayName("Minh Tran")
                .avatarUrl("/avatars/minh.webp")
                .verified(true)
                .build());

        AuthorResponse author = new AsyncHelper(profileProvider, uploadImageFile)
                .getProfileAsync("creator-1")
                .get();

        assertThat(author).isNotNull();
        assertThat(author.isVerified()).isTrue();
        assertThat(author.getUserId()).isEqualTo("creator-1");
    }
}
