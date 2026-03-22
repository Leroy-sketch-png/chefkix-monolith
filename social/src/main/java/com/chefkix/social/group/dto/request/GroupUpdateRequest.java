package com.chefkix.social.group.dto.request;

import lombok.Data;

@Data
public class GroupUpdateRequest {
    private String name;
    private String description;
    private String coverImageUrl;
}