package com.chefkix.social.story.entity;

import lombok.Data;

import java.util.Map;

@Data
public class StoryItem {
    String type;

    double x;
    double y;
    double rotation;
    double scale;

    Map<String, Object> data;
}