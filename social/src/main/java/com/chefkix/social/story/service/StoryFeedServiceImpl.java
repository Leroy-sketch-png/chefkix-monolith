package com.chefkix.social.story.service;

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

    @Override
    public List<UserStoryFeedResponse> getStoryFeed(String currentUserId) {
        List<String> followingIds = profileProvider.getFollowingIds(currentUserId);

        Set<String> targetUserIds = new HashSet<>();
        if (followingIds != null && !followingIds.isEmpty()) {
            targetUserIds.addAll(followingIds);
        }
        targetUserIds.add(currentUserId);

        List<Story> activeStories = storyRepository
                .findByUserIdInAndIsDeletedFalseAndExpiresAtAfterOrderByCreatedAtAsc(targetUserIds, Instant.now());

if (activeStories.isEmpty()) return new ArrayList<>();


        Map<String, Long> totalStoriesPerAuthor = activeStories.stream()
                .collect(Collectors.groupingBy(Story::getUserId, Collectors.counting()));

        List<String> allActiveStoryIds = activeStories.stream().map(Story::getId).toList();

        List<StoryInteraction> myViews = storyInteractionRepository
                .findByUserIdAndStoryIdInAndIsViewedTrue(currentUserId, allActiveStoryIds);

        Set<String> viewedStoryIds = myViews.stream().map(StoryInteraction::getStoryId).collect(Collectors.toSet());

        Map<String, Long> viewedStoriesPerAuthor = activeStories.stream()
                .filter(story -> viewedStoryIds.contains(story.getId()))
                .collect(Collectors.groupingBy(Story::getUserId, Collectors.counting()));

        List<UserStoryFeedResponse> feed = new ArrayList<>();

        for (String authorId : totalStoriesPerAuthor.keySet()) {
            BasicProfileInfo info = profileProvider.getBasicProfile(authorId);

            long total = totalStoriesPerAuthor.getOrDefault(authorId, 0L);
            long viewed = viewedStoriesPerAuthor.getOrDefault(authorId, 0L);

            boolean hasUnseen = !authorId.equals(currentUserId) && (viewed < total);

            feed.add(new UserStoryFeedResponse(
                    authorId,
                    info != null ? info.getDisplayName() : "Anonymous user",
                    info != null ? info.getAvatarUrl() : null,
                    hasUnseen
            ));
        }

        feed.sort((a, b) -> {
            boolean isAMe = a.userId().equals(currentUserId);
            boolean isBMe = b.userId().equals(currentUserId);

            if (isAMe && !isBMe) return -1;
            if (!isAMe && isBMe) return 1;

            return Boolean.compare(b.hasUnseenStory(), a.hasUnseenStory());
        });

        return feed;
    }


    @Override
    public List<StoryResponse> getUserActiveStories(String currentUserId, String targetUserId) {
        List<Story> stories = storyRepository
                .findByUserIdAndIsDeletedFalseAndExpiresAtAfterOrderByCreatedAtAsc(targetUserId, Instant.now());

        if (stories.isEmpty()) return List.of();

        if (currentUserId.equals(targetUserId)) {
            return stories.stream().map(storyMapper::toStoryResponse).toList();
        }


        return stories.stream()
                .map(storyMapper::toStoryResponse)
                .toList();
    }

    public StoryResponse getStoryById(String id) {
        Story story = storyRepository.findByIdAndIsDeletedFalseAndExpiresAtAfter(id, Instant.now())
                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));

        return storyMapper.toStoryResponse(story);
    }
}
