package com.chefkix.social.story.entity;

import lombok.Data;

import java.util.Map;

@Data
public class StoryItem {
    // Phân loại: TEXT, HASHTAG, LOCATION, LINK, STICKER
    String type;

    // Tọa độ để hiển thị trên màn hình (0.0 đến 1.0)
    double x;
    double y;
    double rotation;
    double scale;

    // Dữ liệu tùy biến theo loại
    // Ví dụ: { "text": "Ngon quá", "color": "#FFFFFF" }
    // Hoặc: { "url": "https://...", "title": "Xem công thức" }
    // Hoặc: { "lat": 10.1, "lng": 106.2, "address": "TP.HCM" }
    Map<String, Object> data;
}