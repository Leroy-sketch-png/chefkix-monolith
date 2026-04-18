package com.chefkix.social.story.controller;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.shared.util.UploadImageFile;
import com.chefkix.social.story.dto.request.StoryCreateRequest;
import com.chefkix.social.story.dto.response.StoryResponse;
import com.chefkix.social.story.service.StoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.util.List;

@RestController
@RequestMapping("/stories")
@RequiredArgsConstructor
@Validated
public class StoryController {
    private final StoryService storyService;
    private final UploadImageFile uploadImageFile;

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StoryResponse> create(
            @RequestPart("file") MultipartFile file, // Hứng file ảnh thật
            @RequestPart("story") @Valid StoryCreateRequest request // Hứng metadata (Stickers, Text...)
    ) {
        // 1. Upload file lên Cloudinary và lấy URL về
        // Giả sử hàm upload của bạn trả về String URL
        String uploadedUrl = uploadImageFile.uploadImageFile(file);

        // 2. Tạo Story với URL vừa lấy được
        // Bạn cần update StoryCreateRequest hoặc Map nó sang Entity với mediaUrl là uploadedUrl
        StoryResponse response = storyService.createStory(getCurrentUserId(), request, uploadedUrl);

        return ApiResponse.created(response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable String id) {
        storyService.deleteStory(getCurrentUserId(), id);
        return ApiResponse.success("Story deleted");
    }

    @GetMapping("/me/active")
    public ApiResponse<List<StoryResponse>> getActive() {
        return ApiResponse.success(storyService.getMyActiveStories(getCurrentUserId()));
    }

    @GetMapping("/me/archive")
    public ApiResponse<Page<StoryResponse>> getArchive(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(storyService.getMyArchivedStories(getCurrentUserId(), page, size));
    }

    @PatchMapping("/{storyId}/archive")
    public ApiResponse<String> archiveStoryEarly(@PathVariable String storyId) {
        storyService.archiveStoryEarly(storyId, getCurrentUserId());
        return ApiResponse.success("Story archived");
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}