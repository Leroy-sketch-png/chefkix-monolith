package com.chefkix.social.story.service;

// ... (các import)
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.social.story.dto.response.StoryResponse;
import com.chefkix.social.story.dto.response.UserStoryFeedResponse;
import com.chefkix.social.story.entity.Story;
import com.chefkix.social.story.mapper.StoryMapper;
import com.chefkix.social.story.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class StoryFeedServiceImpl implements StoryFeedService {

    private final StoryRepository storyRepository;
    private final StoryMapper storyMapper;
    private final ProfileProvider profileProvider;

    @Override
    public List<UserStoryFeedResponse> getStoryFeed(String currentUserId) {
        // 1. Lấy danh sách đang follow
        List<String> followingIds = profileProvider.getFollowingIds(currentUserId);
        if (followingIds == null || followingIds.isEmpty()) return List.of();

        // 2. Chỉ lấy ra ID của những người CÓ STORY (Query siêu nhẹ)
        List<String> usersWithStoryIds = storyRepository.findUserIdsWithActiveStories(followingIds, Instant.now());

        // 3. Build Response gửi về cho Frontend vẽ vòng tròn
        List<UserStoryFeedResponse> feed = new ArrayList<>();

        // Gọi 1 hàm lấy Profile của tất cả người này cùng lúc (Tránh N+1 Query)
        // Map<String, BasicProfileInfo> profiles = profileProvider.getBasicProfiles(usersWithStoryIds);

        for (String authorId : usersWithStoryIds) {
            BasicProfileInfo info = profileProvider.getBasicProfile(authorId);
            feed.add(new UserStoryFeedResponse(
                    authorId,
                    info.getDisplayName(),
                    info.getAvatarUrl(),
                    true // Tạm thời để true (có story), nếu có bảng View thì check xem user hiện tại đã xem chưa
            ));
        }

        return feed;
    }

    @Override
    public List<StoryResponse> getUserActiveStories(String currentUserId, String targetUserId) {
        // 1. Lấy tất cả active story của targetUser
        List<Story> stories = storyRepository
                .findByUserIdAndIsDeletedFalseAndExpiresAtAfterOrderByCreatedAtAsc(targetUserId, Instant.now());

        // Nếu họ không có story, trả về rỗng
        if (stories.isEmpty()) return List.of();

        // 2. Nếu currentUserId đang tự xem mình, trả về hết (không cần lọc bạn thân)
        if (currentUserId.equals(targetUserId)) {
            return stories.stream().map(storyMapper::toStoryResponse).toList();
        }

        // 3. Nếu đang xem người khác, phải kiểm tra xem mình có phải bạn thân không
        //Set<String> closeFriendOfUserIds = profileProvider.getUsersWhoAddedMeAsCloseFriend(currentUserId);
        //boolean amICloseFriend = closeFriendOfUserIds.contains(targetUserId);

        return stories.stream()
                //.filter(story -> !story.getIsCloseFriendsOnly() || amICloseFriend)
                .map(storyMapper::toStoryResponse)
                .toList();
    }
}