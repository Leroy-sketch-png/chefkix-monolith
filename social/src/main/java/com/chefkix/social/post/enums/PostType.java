package com.chefkix.social.post.enums;

public enum PostType {
    PERSONAL,    // Shows up on their personal profile and followers' feeds
    GROUP,       // Shows up ONLY inside the specific Group feed
    QUICK,       // Lightweight 2-tap post (photo + caption, no cooking session)
    POLL,        // Two-option poll ("Carbonara: cream or no cream?")
    RECENT_COOK  // Auto-draft: lightweight feed item after completing a cooking session
}