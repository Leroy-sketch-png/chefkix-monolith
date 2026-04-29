package com.chefkix.social.story.service;

// ... (các import)
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.story.dto.response.StoryResponse;
import com.chefkix.social.story.dto.response.UserStoryFeedResponse;
import com.chefkix.social.story.entity.Story;
import com.chefkix.social.story.entity.StoryInteraction;
import com.chefkix.social.story.mapper.StoryMapper;
import com.chefkix.social.story.repository.StoryInteractionRepository;
import com.chefkix.social.story.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryFeedServiceImpl implements StoryFeedService {

    private final StoryRepository storyRepository;
    private final StoryInteractionRepository  storyInteractionRepository;
    private final StoryMapper storyMapper;
    private final ProfileProvider profileProvider;

//    @Override
//    public List<UserStoryFeedResponse> getStoryFeed(String currentUserId) {
//        // 1. Lấy danh sách đang follow
//        List<String> followingIds = profileProvider.getFollowingIds(currentUserId);
//        if (followingIds == null || followingIds.isEmpty()) return List.of();
//
//        // 2. Chỉ lấy ra ID của những người CÓ STORY (Query siêu nhẹ)
//        List<String> usersWithStoryIds = storyRepository.findUserIdsWithActiveStories(followingIds, Instant.now());
//
//        // 3. Build Response gửi về cho Frontend vẽ vòng tròn
//        List<UserStoryFeedResponse> feed = new ArrayList<>();
//
//        // Gọi 1 hàm lấy Profile của tất cả người này cùng lúc (Tránh N+1 Query)
//        // Map<String, BasicProfileInfo> profiles = profileProvider.getBasicProfiles(usersWithStoryIds);
//        boolean isViewed = storyInteractionRepository.findByUserIdAndStoryIdInAndIsViewedTrue()
//
//        for (String authorId : usersWithStoryIds) {
//            BasicProfileInfo info = profileProvider.getBasicProfile(authorId);
//            feed.add(new UserStoryFeedResponse(
//                    authorId,
//                    info.getDisplayName(),
//                    info.getAvatarUrl(),
//                    true // Tạm thời để true (có story), nếu có bảng View thì check xem user hiện tại đã xem chưa
//            ));
//        }
//
//        return feed;
//    }

    @Override
    public List<UserStoryFeedResponse> getStoryFeed(String currentUserId) {
        // 1. Lấy danh sách đang follow
        List<String> followingIds = profileProvider.getFollowingIds(currentUserId);

        // 🌟 Dùng Set để tránh bị trùng ID nếu lỡ có lưu "tự follow bản thân" trong DB
        Set<String> targetUserIds = new HashSet<>();
        if (followingIds != null && !followingIds.isEmpty()) {
            targetUserIds.addAll(followingIds);
        }
        // Luôn luôn nhét bản thân vào mảng để query
        targetUserIds.add(currentUserId);

        // 2. Lấy TẤT CẢ Story còn hạn của những người này
        List<Story> activeStories = storyRepository
                .findByUserIdInAndIsDeletedFalseAndExpiresAtAfterOrderByCreatedAtAsc(targetUserIds, Instant.now());

        if (activeStories.isEmpty()) return new ArrayList<>(); // Trả về list rỗng an toàn

        // --- BẮT ĐẦU LOGIC TÍNH TOÁN SEEN/UNSEEN TRÊN RAM ---

        // 2.1. Gom nhóm: Tác giả này có bao nhiêu Story còn hạn?
        Map<String, Long> totalStoriesPerAuthor = activeStories.stream()
                .collect(Collectors.groupingBy(Story::getUserId, Collectors.counting()));

        // 2.2. Lấy danh sách ID của tất cả các Story này
        List<String> allActiveStoryIds = activeStories.stream().map(Story::getId).toList();

        // 2.3. Query DB 1 lần duy nhất: Trong mảng ID trên, currentUserId ĐÃ XEM những cái nào?
        List<StoryInteraction> myViews = storyInteractionRepository
                .findByUserIdAndStoryIdInAndIsViewedTrue(currentUserId, allActiveStoryIds);

        // Chuyển thành Set cho tốc độ tra cứu O(1)
        Set<String> viewedStoryIds = myViews.stream().map(StoryInteraction::getStoryId).collect(Collectors.toSet());

        // 2.4. Gom nhóm: Tác giả này có bao nhiêu Story MÀ MÌNH ĐÃ XEM?
        Map<String, Long> viewedStoriesPerAuthor = activeStories.stream()
                .filter(story -> viewedStoryIds.contains(story.getId()))
                .collect(Collectors.groupingBy(Story::getUserId, Collectors.counting()));

        // 3. Build Response gửi về cho Frontend
        List<UserStoryFeedResponse> feed = new ArrayList<>();

        for (String authorId : totalStoriesPerAuthor.keySet()) {
            BasicProfileInfo info = profileProvider.getBasicProfile(authorId);

            long total = totalStoriesPerAuthor.getOrDefault(authorId, 0L);
            long viewed = viewedStoriesPerAuthor.getOrDefault(authorId, 0L);

            // ĐIỂM ĂN TIỀN: Story của BẢN THÂN thì luôn coi là đã xem (hasUnseen = false)
            boolean hasUnseen = !authorId.equals(currentUserId) && (viewed < total);

            feed.add(new UserStoryFeedResponse(
                    authorId,
                    info != null ? info.getDisplayName() : "Anonymous user",
                    info != null ? info.getAvatarUrl() : null,
                    hasUnseen
            ));
        }

        // 4. 🌟 Sắp xếp Bảng tin GỘP CHUNG (An toàn tuyệt đối)
        feed.sort((a, b) -> {
            boolean isAMe = a.userId().equals(currentUserId);
            boolean isBMe = b.userId().equals(currentUserId);

            // Ưu tiên 1: Bản thân phải luôn lên đầu (Return -1 nghĩa là a đứng trước b)
            if (isAMe && !isBMe) return -1;
            if (!isAMe && isBMe) return 1;

            // Ưu tiên 2: Những người khác thì ai CHƯA XEM (true) xếp trước, ĐÃ XEM (false) xếp sau
            return Boolean.compare(b.hasUnseenStory(), a.hasUnseenStory());
        });

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

    public StoryResponse getStoryById(String id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));

        return storyMapper.toStoryResponse(story);
    }
}