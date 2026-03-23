package com.chefkix.social.provider;

import com.chefkix.social.api.PostProvider;
import com.chefkix.social.api.dto.PostDetail;
import com.chefkix.social.api.dto.PostLinkInfo;
import com.chefkix.social.api.dto.PostSummary;
import com.chefkix.social.api.dto.RecentCookRequest;
import com.chefkix.social.post.dto.response.PostResponse;
import com.chefkix.social.post.repository.PostRepository;
import com.chefkix.social.post.service.PostService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Implements {@link PostProvider} for cross-module consumption.
 * Delegates to PostService and maps internal DTOs to API contract types.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostProviderImpl implements PostProvider {

    PostService postService;
    PostRepository postRepository;

    @Override
    public Page<PostSummary> getPostsByUserId(String userId, Pageable pageable) {
        Page<PostResponse> posts = postService.getAllPostsByUserId(userId, pageable, null);
        return posts.map(this::toPostSummary);
    }

    @Override
    public long countPostsByUserId(String userId) {
        return postRepository.countByUserIdAndHiddenFalse(userId);
    }

    @Override
    public PostLinkInfo getPostLinking(String postId) {
        return postService.linkingResponse(postId);
    }

    @Override
    public void updatePostXp(String postId, double xpAmount) {
        postService.updatePostXpEarned(postId, xpAmount);
    }

    @Override
    public PostDetail getPostDetail(String postId) {
        return postService.getPostDetail(postId);
    }

    @Override
    public void createRecentCookPost(RecentCookRequest request) {
        postService.createRecentCookPost(request);
    }

    private PostSummary toPostSummary(PostResponse post) {
        return PostSummary.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .displayName(post.getDisplayName())
                .avatarUrl(post.getAvatarUrl())
                .content(post.getContent())
                .slug(post.getSlug())
                .photoUrls(post.getPhotoUrls())
                .videoUrl(post.getVideoUrl())
                .postUrl(post.getPostUrl())
                .tags(post.getTags())
                .sessionId(post.getSessionId())
                .recipeId(post.getRecipeId())
                .recipeTitle(post.getRecipeTitle())
                .privateRecipe(post.isPrivateRecipe())
                .xpEarned(post.getXpEarned())
                .likes(post.getLikes())
                .commentCount(post.getCommentCount())
                .liked(post.getIsLiked())
                .saved(post.getIsSaved())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
