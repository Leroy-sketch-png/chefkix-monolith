package com.chefkix.social.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.api.ContentModerationProvider;
import com.chefkix.culinary.api.RecipeProvider;
import com.chefkix.culinary.api.SessionProvider;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.shared.event.PostDeletedEvent;
import com.chefkix.shared.exception.AppException;
import com.chefkix.social.group.entity.Group;
import com.chefkix.social.group.enums.PrivacyType;
import com.chefkix.social.group.repository.GroupMemberRepository;
import com.chefkix.social.group.repository.GroupRepository;
import com.chefkix.social.post.dto.response.PostResponse;
import com.chefkix.social.post.entity.Collection;
import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.enums.PostStatus;
import com.chefkix.social.post.enums.PostType;
import com.chefkix.social.post.repository.BattleVoteRepository;
import com.chefkix.social.post.repository.CollectionRepository;
import com.chefkix.social.post.repository.CommentLikeRepository;
import com.chefkix.social.post.repository.CommentRepository;
import com.chefkix.social.post.repository.PlateRatingRepository;
import com.chefkix.social.post.repository.PollVoteRepository;
import com.chefkix.social.post.repository.PostLikeRepository;
import com.chefkix.social.post.repository.PostRepository;
import com.chefkix.social.post.repository.PostSaveRepository;
import com.chefkix.social.post.repository.ReplyLikeRepository;
import com.chefkix.social.post.repository.ReplyRepository;
import com.chefkix.social.post.repository.ReportRepository;
import com.chefkix.social.post.mapper.PostMapper;
import com.chefkix.shared.util.UploadImageFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostMapper postMapper;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private PostSaveRepository postSaveRepository;
    @Mock
    private PollVoteRepository pollVoteRepository;
    @Mock
    private PlateRatingRepository plateRatingRepository;
    @Mock
    private BattleVoteRepository battleVoteRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ReplyRepository replyRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private ReplyLikeRepository replyLikeRepository;
    @Mock
    private CollectionRepository collectionRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private UploadImageFile uploadImageFile;
    @Mock
    private ProfileProvider profileProvider;
    @Mock
    private SessionProvider sessionProvider;
    @Mock
    private ContentModerationProvider contentModerationProvider;
    @Mock
    private RecipeProvider recipeProvider;
    @Mock
    private Executor taskExecutor;

    @InjectMocks
    private PostService postService;

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
    void deletePostRemovesCollectionAndReportReferencesBeforePublishingDeleteEvent() {
        String postId = "post-1";
        Post post = Post.builder()
                .id(postId)
                .userId("user-1")
                .build();
        Collection collection = Collection.builder()
                .id("collection-1")
                .userId("user-2")
                .postIds(new ArrayList<>(List.of(postId, "post-2")))
                .itemCount(2)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findByPostId(postId)).thenReturn(List.of());
        when(collectionRepository.findAllByPostIdsContaining(postId)).thenReturn(List.of(collection));

        postService.deletePost(SecurityContextHolder.getContext().getAuthentication(), postId);

        verify(postLikeRepository).deleteAllByPostId(postId);
        verify(postSaveRepository).deleteAllByPostId(postId);
        verify(pollVoteRepository).deleteAllByPostId(postId);
        verify(plateRatingRepository).deleteAllByPostId(postId);
        verify(battleVoteRepository).deleteAllByPostId(postId);
        verify(commentRepository).deleteAllByPostId(postId);
        verify(collectionRepository).saveAll(List.of(collection));
        verify(reportRepository).deleteAllByTargetTypeAndTargetId("post", postId);
        verify(postRepository).delete(post);
        verify(eventPublisher).publishEvent(any(Object.class));

        ArgumentCaptor<PostDeletedEvent> eventCaptor = ArgumentCaptor.forClass(PostDeletedEvent.class);
        verify(kafkaTemplate).send(eq("post-deleted-delivery"), eventCaptor.capture());

        PostDeletedEvent emittedEvent = eventCaptor.getValue();
        assertThat(emittedEvent.getPostId()).isEqualTo(postId);
        assertThat(emittedEvent.getUserId()).isEqualTo("user-1");
        assertThat(collection.getPostIds()).containsExactly("post-2");
        assertThat(collection.getItemCount()).isEqualTo(1);
    }

    @Test
    void getGroupPostsReturnsScopedPostsForAuthorizedViewer() {
        String groupId = "group-1";
        String currentUserId = "user-1";
        var pageable = PageRequest.of(0, 10);

        Group group = Group.builder()
                .id(groupId)
                .privacyType(PrivacyType.PUBLIC)
                .build();
        Post post = Post.builder()
                .id("post-1")
                .groupId(groupId)
                .postType(PostType.GROUP)
                .status(PostStatus.ACTIVE)
                .build();
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setPostType(PostType.GROUP);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)).thenReturn(Optional.empty());
        when(postRepository.findByGroupIdAndPostTypeAndStatusAndHiddenFalseOrderByCreatedAtDesc(
                groupId,
                PostType.GROUP,
                PostStatus.ACTIVE,
                pageable)).thenReturn(new PageImpl<>(List.of(post), pageable, 1));
        when(postMapper.toPostResponse(post)).thenReturn(response);
        when(postLikeRepository.findByUserIdAndPostIdIn(currentUserId, List.of(post.getId()))).thenReturn(List.of());
        when(postSaveRepository.findByUserIdAndPostIdIn(currentUserId, List.of(post.getId()))).thenReturn(List.of());
        when(plateRatingRepository.findByPostIdInAndUserId(List.of(post.getId()), currentUserId)).thenReturn(List.of());

        var result = postService.getGroupPosts(groupId, pageable, currentUserId);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(post.getId());
        verify(postRepository).findByGroupIdAndPostTypeAndStatusAndHiddenFalseOrderByCreatedAtDesc(
                groupId,
                PostType.GROUP,
                PostStatus.ACTIVE,
                pageable);
    }

    @Test
    void getGroupPostsRejectsPrivateGroupVisitorsWithoutActiveMembership() {
        String groupId = "group-1";
        String currentUserId = "user-1";
        var pageable = PageRequest.of(0, 10);

        Group group = Group.builder()
                .id(groupId)
                .privacyType(PrivacyType.PRIVATE)
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getGroupPosts(groupId, pageable, currentUserId))
                .isInstanceOf(AppException.class);

        verify(postRepository, never()).findByGroupIdAndPostTypeAndStatusAndHiddenFalseOrderByCreatedAtDesc(
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void getAllPostsForYouBuildsCandidateQueryAgainstStatusFieldOnly() {
        String currentUserId = "user-1";
        var pageable = PageRequest.of(0, 10);
        Post candidate = Post.builder()
                .id("candidate-1")
                .userId("chef-2")
                .postType(PostType.PERSONAL)
                .status(PostStatus.ACTIVE)
                .tags(List.of("ramen"))
                .createdAt(java.time.Instant.now())
                .hotScore(12.0)
                .build();
        PostResponse response = new PostResponse();
        response.setId(candidate.getId());
        response.setPostType(PostType.PERSONAL);

        when(mongoTemplate.find(any(Query.class), eq(com.chefkix.social.post.entity.PostLike.class))).thenReturn(List.of());
        when(mongoTemplate.find(any(Query.class), eq(com.chefkix.social.post.entity.PostSave.class))).thenReturn(List.of());
        when(profileProvider.getUserPreferences(currentUserId)).thenReturn(List.of("ramen"));
        when(mongoTemplate.find(any(Query.class), eq(Post.class))).thenReturn(List.of(candidate));
        when(postMapper.toPostResponse(candidate)).thenReturn(response);
        when(postLikeRepository.findByUserIdAndPostIdIn(currentUserId, List.of(candidate.getId()))).thenReturn(List.of());
        when(postSaveRepository.findByUserIdAndPostIdIn(currentUserId, List.of(candidate.getId()))).thenReturn(List.of());
        when(plateRatingRepository.findByPostIdInAndUserId(List.of(candidate.getId()), currentUserId)).thenReturn(List.of());

        Page<PostResponse> result = postService.getAllPosts(2, pageable, currentUserId);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(Post.class));

        assertThat(result.getContent()).hasSize(1);
        assertThat(queryCaptor.getValue().getQueryObject().containsKey("status")).isTrue();
        assertThat(queryCaptor.getValue().getQueryObject().containsKey("postStatus")).isFalse();
    }
}