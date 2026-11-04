package com.chefkix.social.policy;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BlockedContentPolicy {

    private final ProfileProvider profileProvider;

    public BlockedContentPolicy(ProfileProvider profileProvider) {
        this.profileProvider = profileProvider;
    }

    public List<String> invisibleUserIds(String viewerId) {
        if (!StringUtils.hasText(viewerId)) {
            return List.of();
        }
        List<String> invisibleIds = profileProvider.getInvisibleUserIds(viewerId);
        if (invisibleIds == null) {
            throw new IllegalStateException("Block visibility lookup returned no result");
        }
        if (invisibleIds.isEmpty()) {
            return List.of();
        }
        return invisibleIds.stream().filter(Objects::nonNull).distinct().toList();
    }

    public void requireVisible(String viewerId, String authorId) {
        if (!StringUtils.hasText(viewerId)
                || !StringUtils.hasText(authorId)
                || viewerId.equals(authorId)) {
            return;
        }
        if (profileProvider.isBlocked(viewerId, authorId)) {
            throw new AppException(ErrorCode.POST_NOT_FOUND);
        }
    }
}
