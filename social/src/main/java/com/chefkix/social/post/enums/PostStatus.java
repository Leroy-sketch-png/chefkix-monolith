package com.chefkix.social.post.enums;

public enum PostStatus {
    ACTIVE,   // Visible to everyone
    PENDING,  // Waiting for a Group Admin to approve it
    HIDDEN,   // Auto-hidden due to reports
    DELETED   // Soft-deleted by user or admin
}