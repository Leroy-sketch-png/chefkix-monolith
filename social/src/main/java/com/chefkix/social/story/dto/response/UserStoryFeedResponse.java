package com.chefkix.social.story.dto.response;

import java.util.List;

// Gói gọn thông tin của 1 User và toàn bộ Story còn hạn của họ
public record UserStoryFeedResponse(
        String userId,
        String displayName, // Tên hiển thị (Lấy từ User Service)
        String avatarUrl,   // Ảnh đại diện (Lấy từ User Service)
        boolean hasUnseenStory
) {}