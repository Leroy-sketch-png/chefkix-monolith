package com.chefkix.social.story.service;

import com.chefkix.social.story.dto.request.HighlightCreateRequest;
import com.chefkix.social.story.dto.request.HighlightUpdateRequest;
import com.chefkix.social.story.dto.response.HighlightResponse;
import com.chefkix.social.story.dto.response.StoryResponse;
import com.chefkix.social.story.entity.Story;
import com.chefkix.social.story.entity.StoryHighlight;
import com.chefkix.social.story.mapper.StoryHighlightMapper;
import com.chefkix.social.story.mapper.StoryMapper;
import com.chefkix.social.story.repository.StoryHighlightRepository;
import com.chefkix.social.story.repository.StoryRepository;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.policy.BlockedContentPolicy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoryHighlightServiceImpl implements StoryHighlightService {

    StoryHighlightRepository highlightRepo;
    StoryRepository storyRepo;
    StoryMapper storyMapper;
    StoryHighlightMapper storyHighlightMapper;
    BlockedContentPolicy blockedContentPolicy;

    @Override
    public HighlightResponse createHighlight(String userId, HighlightCreateRequest request) {
        validateStoryOwnership(request.storyIds(), userId);
        String title = StringUtils.hasText(request.title()) ? request.title().trim() : "highlights";

        StoryHighlight highlight = StoryHighlight.builder()
                .userId(userId)
                .title(title)
                .coverUrl(request.coverUrl())
                .storyIds(request.storyIds())
                .createdAt(Instant.now())
                .build();

        highlightRepo.save(highlight);
        return storyHighlightMapper.toHighlightResponse(highlight);
    }

    @Override
    public List<HighlightResponse> getUserHighlights(String currentUserId, String targetUserId) {
        blockedContentPolicy.requireVisible(currentUserId, targetUserId);
        List<StoryHighlight> highlights = highlightRepo.findByUserIdOrderByCreatedAtDesc(targetUserId);

        return highlights.stream()
                .map(storyHighlightMapper::toHighlightResponse)
                .toList();
    }

    @Override
    public List<StoryResponse> getStoriesInHighlight(String currentUserId, String highlightId) {
        StoryHighlight highlight = highlightRepo.findById(highlightId)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_HIGHLIGHT_NOT_FOUND));
        blockedContentPolicy.requireVisible(currentUserId, highlight.getUserId());

        List<Story> stories = storyRepo.findByIdInAndIsDeletedFalse(highlight.getStoryIds());

        Map<String, Story> storyMap = stories.stream()
                .collect(Collectors.toMap(Story::getId, s -> s));
        List<String> invisibleUserIds = blockedContentPolicy.invisibleUserIds(currentUserId);

        return highlight.getStoryIds().stream()
                .filter(storyMap::containsKey)
                .map(storyMap::get)
                .filter(story -> !invisibleUserIds.contains(story.getUserId()))
                .map(storyMapper::toStoryResponse)
                .toList();
    }

    @Override
    public void updateHighlight(String highlightId, String currentUserId, HighlightUpdateRequest request) {
        StoryHighlight highlight = getHighlightAndVerifyOwner(highlightId, currentUserId);

        if (request.storyIds() != null) {
            validateStoryOwnership(request.storyIds(), currentUserId);
            highlight.setStoryIds(request.storyIds());
        }

        if (request.title() != null && !request.title().isBlank()) {
            highlight.setTitle(request.title());
        }
        if (request.coverUrl() != null) {
            highlight.setCoverUrl(request.coverUrl());
        }

        highlightRepo.save(highlight);
    }

    @Override
    public void deleteHighlight(String highlightId, String currentUserId) {
        StoryHighlight highlight = getHighlightAndVerifyOwner(highlightId, currentUserId);
highlightRepo.delete(highlight);
    }


    private void validateStoryOwnership(List<String> storyIds, String userId) {
        if (storyIds == null || storyIds.isEmpty()) return;
        long validCount = storyRepo.countByIdInAndUserId(storyIds, userId);
        if (validCount != storyIds.size()) {
            throw new AppException(ErrorCode.STORY_HIGHLIGHT_STORIES_INVALID);
        }
    }

    private StoryHighlight getHighlightAndVerifyOwner(String highlightId, String userId) {
        StoryHighlight highlight = highlightRepo.findById(highlightId)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_HIGHLIGHT_NOT_FOUND));
        if (!highlight.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.STORY_HIGHLIGHT_NOT_FOUND);
        }
        return highlight;
    }
}
