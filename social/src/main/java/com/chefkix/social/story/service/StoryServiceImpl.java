package com.chefkix.social.story.service;

import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.story.dto.request.StoryCreateRequest;
import com.chefkix.social.story.dto.response.StoryResponse;
import com.chefkix.social.story.entity.Story;
import com.chefkix.social.story.mapper.StoryMapper;
import com.chefkix.social.story.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryServiceImpl implements StoryService {
    private final StoryRepository storyRepository;
    private final StoryMapper storyMapper;

    @Override
    public StoryResponse createStory(String userId, StoryCreateRequest request) {
        Instant now = Instant.now();
        Story story = Story.builder()
                .userId(userId)
                .mediaUrl(request.mediaUrl())
                .mediaType(request.mediaType())
                .items(storyMapper.toStoryItems(request.items()))
                .createdAt(now)
                .expiresAt(now.plus(24, ChronoUnit.HOURS))
                .isDeleted(false)
                .build();
        return storyMapper.toStoryResponse(storyRepository.save(story));
    }

    @Override
    public void deleteStory(String userId, String storyId) {
//        // Fallback: Nếu không tìm thấy hoặc đã xóa rồi thì báo lỗi 404
//        Story story = storyRepository.findByIdAndUserIdAndIsDeletedFalse(storyId, userId)
//                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));
//
//        // Thực hiện Soft Delete: Đưa vào trạng thái 'mất hẳn' đối với người dùng
//        story.setIsDeleted(true);
//        storyRepository.save(story);
    }

    @Override
    public List<StoryResponse> getMyActiveStories(String userId) {
//        return storyRepository.findByUserIdAndIsDeletedFalseAndExpiresAtAfterOrderByCreatedAtAsc(userId, Instant.now())
//                .stream().map(storyMapper::toStoryResponse).toList();
        return null;
    }

    @Override
    public Page<StoryResponse> getMyArchivedStories(String userId, int page, int size) {
//        return storyRepository.findByUserIdAndIsDeletedFalseAndExpiresAtBeforeOrderByCreatedAtDesc(
//                        userId, Instant.now(), PageRequest.of(page, size))
//                .map(storyMapper::toStoryResponse);
        return null;
    }
}