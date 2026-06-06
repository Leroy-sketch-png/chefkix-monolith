package com.chefkix.social.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.api.RecipeProvider;
import com.chefkix.culinary.api.dto.RecipeSummaryInfo;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.social.post.dto.request.CollectionRequest;
import com.chefkix.social.post.entity.Collection;
import com.chefkix.social.post.entity.DifficultyStep;
import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.mapper.PostMapper;
import com.chefkix.social.post.repository.CollectionProgressRepository;
import com.chefkix.social.post.repository.CollectionRepository;
import com.chefkix.social.post.repository.PostRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CollectionServiceTest {

    @Mock
    private CollectionRepository collectionRepository;
    @Mock
    private CollectionProgressRepository collectionProgressRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostMapper postMapper;
    @Mock
    private RecipeProvider recipeProvider;

    @InjectMocks
    private CollectionService collectionService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", "password"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCollectionPersistsLearningPathFields() {
        when(collectionRepository.countByUserId("user-1")).thenReturn(0L);
        when(recipeProvider.getRecipeSummary("recipe-1")).thenReturn(RecipeSummaryInfo.builder()
                .id("recipe-1")
                .coverImageUrl("https://cdn.example/recipe-1.jpg")
                .build());
        when(recipeProvider.getRecipeSummary("recipe-2")).thenReturn(RecipeSummaryInfo.builder()
                .id("recipe-2")
                .coverImageUrl("https://cdn.example/recipe-2.jpg")
                .build());
        when(collectionRepository.save(any(Collection.class))).thenAnswer(invocation -> {
            Collection collection = invocation.getArgument(0);
            collection.setId("collection-1");
            return collection;
        });

        CollectionRequest request = CollectionRequest.builder()
                .name(" Pasta Mastery ")
                .description(" Learn the fundamentals ")
                .isPublic(true)
                .collectionType("LEARNING_PATH")
                .difficultyProgression(List.of(
                        DifficultyStep.builder()
                                .label("Basics")
                                .difficulty("Beginner")
                                .recipeIds(List.of("recipe-1"))
                                .order(7)
                                .build(),
                        DifficultyStep.builder()
                                .label("Sauces")
                                .difficulty("Intermediate")
                                .recipeIds(List.of("recipe-2"))
                                .order(9)
                                .build()))
                .build();

        var response = collectionService.createCollection(request);

        assertThat(response.getId()).isEqualTo("collection-1");
        assertThat(response.getName()).isEqualTo("Pasta Mastery");
        assertThat(response.getDescription()).isEqualTo("Learn the fundamentals");
        assertThat(response.getCollectionType()).isEqualTo("LEARNING_PATH");
        assertThat(response.getRecipeIds()).containsExactly("recipe-1", "recipe-2");
        assertThat(response.getDifficulty()).isEqualTo("Intermediate");
        assertThat(response.getTotalXp()).isEqualTo(100);
        assertThat(response.getItemCount()).isEqualTo(2);
        assertThat(response.getCoverImageUrl()).isEqualTo("https://cdn.example/recipe-1.jpg");
        assertThat(response.getDifficultyProgression())
                .extracting(DifficultyStep::getOrder)
                .containsExactly(0, 1);
    }

    @Test
    void createCollectionRejectsLearningPathWithUnknownRecipe() {
        when(collectionRepository.countByUserId("user-1")).thenReturn(0L);
        when(recipeProvider.getRecipeSummary("missing-recipe")).thenReturn(null);

        CollectionRequest request = CollectionRequest.builder()
                .name("Broken Path")
                .isPublic(false)
                .collectionType("LEARNING_PATH")
                .recipeIds(List.of("missing-recipe"))
                .build();

        assertThatThrownBy(() -> collectionService.createCollection(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_OPERATION);

        verify(collectionRepository, never()).save(any(Collection.class));
    }

    @Test
    void addPostToCollectionKeepsRecipeCountInItemCount() {
        Collection collection = Collection.builder()
                .id("collection-1")
                .userId("user-1")
                .recipeIds(new ArrayList<>(List.of("recipe-1", "recipe-2")))
                .postIds(new ArrayList<>())
                .itemCount(2)
                .build();
        Post post = Post.builder()
                .id("post-1")
                .photoUrls(List.of("https://cdn.example/post-1.jpg"))
                .build();

        when(collectionRepository.findById("collection-1")).thenReturn(Optional.of(collection));
        when(postRepository.existsById("post-1")).thenReturn(true);
        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
        when(collectionRepository.save(any(Collection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = collectionService.addPostToCollection("collection-1", "post-1");

        assertThat(response.getItemCount()).isEqualTo(3);
        assertThat(response.getPostIds()).containsExactly("post-1");
        assertThat(response.getRecipeIds()).containsExactly("recipe-1", "recipe-2");
        assertThat(response.getCoverImageUrl()).isEqualTo("https://cdn.example/post-1.jpg");
    }

    @Test
    void getCollectionAllowsAnonymousAccessForPublicCollection() {
        Collection collection = Collection.builder()
                .id("collection-1")
                .userId("owner-1")
                .name("Public Path")
                .isPublic(true)
                .build();

        when(collectionRepository.findById("collection-1")).thenReturn(Optional.of(collection));
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "guest-key",
                        "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        var response = collectionService.getCollection("collection-1");

        assertThat(response.getId()).isEqualTo("collection-1");
        assertThat(response.isPublic()).isTrue();
    }

    @Test
    void getCollectionPostsAllowsAnonymousAccessForPublicCollection() {
        Collection collection = Collection.builder()
                .id("collection-1")
                .userId("owner-1")
                .isPublic(true)
                .postIds(List.of("post-1"))
                .build();
        Post post = Post.builder().id("post-1").build();

        when(collectionRepository.findById("collection-1")).thenReturn(Optional.of(collection));
        when(postRepository.findAllById(List.of("post-1"))).thenReturn(List.of(post));
        when(postMapper.toPostResponse(post)).thenReturn(
                com.chefkix.social.post.dto.response.PostResponse.builder().id("post-1").build());
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "guest-key",
                        "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        var response = collectionService.getCollectionPosts("collection-1");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getId()).isEqualTo("post-1");
    }
}