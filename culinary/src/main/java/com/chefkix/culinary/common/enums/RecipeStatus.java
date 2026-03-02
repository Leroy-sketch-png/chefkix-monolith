package com.chefkix.culinary.common.enums;

public enum RecipeStatus {
    DRAFT,      // Đang viết dở
    PENDING,    // Chờ duyệt (nếu có admin)
    PUBLISHED,  // Đã công khai
    ARCHIVED    // Đã xóa mềm
}