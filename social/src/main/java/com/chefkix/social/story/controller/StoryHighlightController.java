package com.chefkix.social.story.controller;

import com.chefkix.social.story.dto.*;
import com.chefkix.social.story.dto.request.HighlightCreateRequest;
import com.chefkix.social.story.service.StoryHighlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StoryHighlightController {

    private final StoryHighlightService highlightService;

    // 1. Tạo Highlight mới
    @PostMapping("/highlights")
    public ResponseEntity<Void> createHighlight(@RequestBody HighlightCreateRequest request) {
        highlightService.createHighlight(getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

//    // 2. Lấy danh sách Highlight của 1 user (HIỆN TRÊN TRANG CÁ NHÂN)
//    @GetMapping("/users/{userId}/highlights")
//    public ResponseEntity<List<HighlightResponse>> getUserHighlights(@PathVariable String userId) {
//        return ResponseEntity.ok(highlightService.getUserHighlights(userId));
//    }
//
//    // 3. Lấy chi tiết các video/ảnh bên trong 1 Highlight (KHI BẤM VÀO ĐỂ XEM)
//    @GetMapping("/highlights/{highlightId}/stories")
//    public ResponseEntity<List<StoryResponse>> getHighlightStories(@PathVariable String highlightId) {
//        return ResponseEntity.ok(highlightService.getStoriesInHighlight(highlightId));
//    }
//
//    // 4. Sửa Highlight (Thêm/bớt story, đổi tên)
//    @PutMapping("/highlights/{highlightId}")
//    public ResponseEntity<Void> updateHighlight(
//            @PathVariable String highlightId,
//            @RequestBody HighlightUpdateRequest request) {
//        highlightService.updateHighlight(highlightId, getCurrentUserId(), request);
//        return ResponseEntity.ok().build();
//    }
//
//    // 5. Xóa Highlight
//    @DeleteMapping("/highlights/{highlightId}")
//    public ResponseEntity<Void> deleteHighlight(@PathVariable String highlightId) {
//        highlightService.deleteHighlight(highlightId, getCurrentUserId());
//        return ResponseEntity.noContent().build();
//    }
//


    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}