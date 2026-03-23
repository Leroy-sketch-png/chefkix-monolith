package com.chefkix.culinary.common.helper;

import com.chefkix.culinary.common.dto.response.AuthorResponse;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.shared.util.UploadImageFile;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AsyncHelper {

    ProfileProvider profileProvider;
    UploadImageFile uploadImageFile;

    /**
     * Fetch profile asynchronously and map to culinary-internal AuthorResponse.
     */
    @Async("taskExecutor")
    public CompletableFuture<AuthorResponse> getProfileAsync(String userId) {
        log.info("[ASYNC-Profile] Fetching profile for user {}", userId);

        BasicProfileInfo profile = profileProvider.getBasicProfile(userId);

        if (profile == null) {
            log.warn("[ASYNC-Profile] User not found {}", userId);
            return CompletableFuture.completedFuture(null);
        }

        AuthorResponse author = AuthorResponse.builder()
                .userId(profile.getUserId())
                .username(profile.getUsername())
                .displayName(profile.getDisplayName())
                .avatarUrl(profile.getAvatarUrl())
                .build();

        return CompletableFuture.completedFuture(author);
    }


    /**
     * Upload file async
     */
    @Async("taskExecutor")
    public CompletableFuture<String> uploadFileAsync(MultipartFile photo) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("[ASYNC-Upload] Upload {}", photo.getOriginalFilename());
            return uploadImageFile.uploadImageFile(photo);
        });
    }
}
