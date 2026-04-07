package com.chefkix.social.story.service;

import com.chefkix.social.story.dto.request.HighlightCreateRequest;
import com.chefkix.social.story.dto.request.HighlightUpdateRequest;
import com.chefkix.social.story.dto.response.HighlightResponse;
import com.chefkix.social.story.dto.response.StoryResponse;
import com.chefkix.social.story.entity.Story;
import com.chefkix.social.story.entity.StoryHighlight;
import com.chefkix.social.story.mapper.StoryMapper;
import com.chefkix.social.story.repository.StoryHighlightRepository;
import com.chefkix.social.story.repository.StoryRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoryHighlightServiceImpl implements StoryHighlightService {

    StoryHighlightRepository highlightRepo;
    StoryRepository storyRepo;
    StoryMapper storyMapper;

    @Override
    public void createHighlight(String userId, HighlightCreateRequest request) {
        // Bảo mật: Xác minh xem tất cả các storyIds này có đúng là của user này không
        validateStoryOwnership(request.storyIds(), userId);

        StoryHighlight highlight = StoryHighlight.builder()
                .userId(userId)
                .title(request.title().isEmpty()?request.title():"highlights")
                .coverUrl(request.coverUrl())
                .storyIds(request.storyIds())
                .createdAt(Instant.now())
                .build();

        highlightRepo.save(highlight);
    }

//    @Override
//    public List<HighlightResponse> getUserHighlights(String targetUserId) {
//        List<StoryHighlight> highlights = highlightRepo.findByUserIdOrderByCreatedAtDesc(targetUserId);
//
//        return highlights.stream()
//                .map(h -> new HighlightResponse(
//                        h.getId(),
//                        h.getTitle(),
//                        h.getCoverUrl(),
//                        h.getStoryIds() != null ? h.getStoryIds().size() : 0
//                ))
//                .toList();
//    }
//
//    @Override
//    public List<StoryResponse> getStoriesInHighlight(String highlightId) {
//        StoryHighlight highlight = highlightRepo.findById(highlightId)
//                .orElseThrow(() -> new RuntimeException("Highlight không tồn tại"));
//
//        // Lấy toàn bộ Story trong mảng (Bỏ qua các story user đã bấm XÓA MỀM)
//        List<Story> stories = storyRepo.findByIdInAndIsDeletedFalse(highlight.getStoryIds());
//
//        // Đảm bảo video được phát đúng thứ tự mà user đã cất công sắp xếp lúc tạo
//        Map<String, Story> storyMap = stories.stream()
//                .collect(Collectors.toMap(Story::getId, s -> s));
//
//        return highlight.getStoryIds().stream()
//                .filter(storyMap::containsKey)
//                .map(storyMap::get)
//                .map(storyMapper::toStoryResponse)
//                .toList();
//    }
//
//    @Override
//    public void updateHighlight(String highlightId, String currentUserId, HighlightUpdateRequest request) {
//        StoryHighlight highlight = getHighlightAndVerifyOwner(highlightId, currentUserId);
//
//        // Nếu user tick chọn thêm/bớt Story
//        if (request.storyIds() != null) {
//            validateStoryOwnership(request.storyIds(), currentUserId);
//            highlight.setStoryIds(request.storyIds());
//        }
//
//        // Đổi tên hoặc ảnh bìa
//        if (request.title() != null && !request.title().isBlank()) {
//            highlight.setTitle(request.title());
//        }
//        if (request.coverUrl() != null) {
//            highlight.setCoverUrl(request.coverUrl());
//        }
//
//        highlightRepo.save(highlight);
//    }
//
//    @Override
//    public void deleteHighlight(String highlightId, String currentUserId) {
//        StoryHighlight highlight = getHighlightAndVerifyOwner(highlightId, currentUserId);
//        highlightRepo.delete(highlight); // Chỉ xóa cái Vỏ (Highlight), Ruột (Story) vẫn còn trong DB
//    }

    // --- INTERNAL HELPERS ---

    private void validateStoryOwnership(List<String> storyIds, String userId) {
        if (storyIds == null || storyIds.isEmpty()) return;
        long validCount = storyRepo.countByIdInAndUserId(storyIds, userId);
        if (validCount != storyIds.size()) {
            throw new IllegalArgumentException("Một số Story không hợp lệ hoặc không thuộc về bạn");
        }
    }

    private StoryHighlight getHighlightAndVerifyOwner(String highlightId, String userId) {
        StoryHighlight highlight = highlightRepo.findById(highlightId)
                .orElseThrow(() -> new RuntimeException("Highlight không tồn tại"));
        if (!highlight.getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thực hiện hành động này");
        }
        return highlight;
    }
}