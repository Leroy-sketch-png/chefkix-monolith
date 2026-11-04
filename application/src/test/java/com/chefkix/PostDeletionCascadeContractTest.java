package com.chefkix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.api.ContentModerationProvider;
import com.chefkix.culinary.api.RecipeProvider;
import com.chefkix.culinary.api.SessionProvider;
import com.chefkix.culinary.features.session.listener.PostDeletedSessionListener;
import com.chefkix.culinary.features.session.service.CookingSessionService;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.listener.PostDeleteListener;
import com.chefkix.identity.service.StatisticsCounterOperations;
import com.chefkix.notification.listener.BellNotificationListener;
import com.chefkix.notification.service.NotificationService;
import com.chefkix.shared.event.PostDeletedEvent;
import com.chefkix.shared.service.KafkaIdempotencyService;
import com.chefkix.shared.util.UploadImageFile;
import com.chefkix.social.group.repository.GroupMemberRepository;
import com.chefkix.social.group.repository.GroupRepository;
import com.chefkix.social.policy.BlockedContentPolicy;
import com.chefkix.social.post.entity.Collection;
import com.chefkix.social.post.entity.Comment;
import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.entity.Reply;
import com.chefkix.social.post.mapper.PostMapper;
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
import com.chefkix.social.post.repository.TasteProfileRedisRepository;
import com.chefkix.social.post.service.PostService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PostDeletionCascadeContractTest {

    // --- Social module mocks ---
    @Mock private PostMapper postMapper;
    @Mock private PostRepository postRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private PostSaveRepository postSaveRepository;
    @Mock private PollVoteRepository pollVoteRepository;
    @Mock private PlateRatingRepository plateRatingRepository;
    @Mock private BattleVoteRepository battleVoteRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private ReplyRepository replyRepository;
    @Mock private CommentLikeRepository commentLikeRepository;
    @Mock private ReplyLikeRepository replyLikeRepository;
    @Mock private CollectionRepository collectionRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private TasteProfileRedisRepository tasteProfileRedisRepository;
    @Mock private BlockedContentPolicy blockedContentPolicy;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UploadImageFile uploadImageFile;
    @Mock private ProfileProvider profileProvider;
    @Mock private SessionProvider sessionProvider;
    @Mock private ContentModerationProvider contentModerationProvider;
    @Mock private RecipeProvider recipeProvider;
    @Mock private Executor taskExecutor;

    @InjectMocks
    private PostService postService;

    // --- Culinary module mocks ---
    @Mock private CookingSessionService cookingSessionService;
    @Mock private KafkaIdempotencyService culinaryIdempotencyService;

    // --- Identity module mocks ---
    @Mock private StatisticsCounterOperations statisticsService;
    @Mock private KafkaIdempotencyService identityIdempotencyService;

    // --- Notification module mocks ---
    @Mock private NotificationService notificationService;
    @Mock private KafkaIdempotencyService notificationIdempotencyService;

    // Listeners under test
    private PostDeletedSessionListener postDeletedSessionListener;
    private PostDeleteListener postDeleteListener;
    private BellNotificationListener bellNotificationListener;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-owner", "password"));
        lenient().when(blockedContentPolicy.invisibleUserIds(any())).thenReturn(List.of());

        postDeletedSessionListener = new PostDeletedSessionListener(
                cookingSessionService,
                culinaryIdempotencyService);

        postDeleteListener = new PostDeleteListener(
                statisticsService,
                identityIdempotencyService);

        bellNotificationListener = new BellNotificationListener(
                notificationService,
                notificationIdempotencyService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("End-to-end post deletion cascade: PostService deletion produces Kafka event that drives culinary, identity, and notification listeners")
    void postDeletionEndToEndCascade_EmittedEventDrivesAllThreeModuleListeners() {
        String postId = "post-99";
        String userId = "user-owner";

        Post post = Post.builder()
                .id(postId)
                .userId(userId)
                .build();

        Comment comment1 = Comment.builder().id("cmt-1").postId(postId).build();
        Reply reply1 = Reply.builder().id("rep-1").parentCommentId("cmt-1").build();

        Collection collection = Collection.builder()
                .id("coll-1")
                .userId(userId)
                .postIds(new ArrayList<>(List.of(postId, "other-post")))
                .recipeIds(new ArrayList<>(List.of("recipe-1", "recipe-2")))
                .itemCount(4)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findByPostId(postId)).thenReturn(List.of(comment1));
        when(replyRepository.findByParentCommentIdIn(List.of("cmt-1"))).thenReturn(List.of(reply1));
        when(collectionRepository.findAllByPostIdsContaining(postId)).thenReturn(List.of(collection));

        // Setup idempotency to accept events for all 3 listeners
        when(culinaryIdempotencyService.tryProcess(anyString(), eq("post-deleted-delivery:culinary-sessions")))
                .thenReturn(true);
        when(identityIdempotencyService.tryProcess(anyString(), eq("post-deleted-delivery:identity")))
                .thenReturn(true);
        when(notificationIdempotencyService.tryProcess(anyString(), eq("post-deleted-delivery:notification")))
                .thenReturn(true);

        when(cookingSessionService.markLinkedPostDeleted(postId)).thenReturn(2);
        when(notificationService.cleanupDeletedPostNotifications(postId)).thenReturn(5L);

        // --- Step 1: Execute PostService.deletePost() ---
        postService.deletePost(SecurityContextHolder.getContext().getAuthentication(), postId);

        // Verify social cascade actions
        verify(postLikeRepository).deleteAllByPostId(postId);
        verify(postSaveRepository).deleteAllByPostId(postId);
        verify(pollVoteRepository).deleteAllByPostId(postId);
        verify(plateRatingRepository).deleteAllByPostId(postId);
        verify(battleVoteRepository).deleteAllByPostId(postId);
        verify(replyLikeRepository).deleteAllByReplyIdIn(List.of("rep-1"));
        verify(replyRepository).deleteAllByParentCommentIdIn(List.of("cmt-1"));
        verify(commentLikeRepository).deleteAllByCommentIdIn(List.of("cmt-1"));
        verify(reportRepository).deleteAllByTargetTypeAndTargetIdIn("comment", List.of("cmt-1"));
        verify(commentRepository).deleteAllByPostId(postId);
        verify(reportRepository).deleteAllByTargetTypeAndTargetId("post", postId);
        verify(postRepository).delete(post);
        verify(eventPublisher).publishEvent(any(Object.class));

        // Verify collection preservation: 1 post left + 2 recipes = 3 items
        assertThat(collection.getPostIds()).containsExactly("other-post");
        assertThat(collection.getItemCount()).isEqualTo(3);
        assertThat(collection.getUpdatedAt()).isNotNull();

        // Capture the emitted PostDeletedEvent
        ArgumentCaptor<PostDeletedEvent> eventCaptor = ArgumentCaptor.forClass(PostDeletedEvent.class);
        verify(kafkaTemplate).send(eq("post-deleted-delivery"), eventCaptor.capture());

        PostDeletedEvent emittedEvent = eventCaptor.getValue();
        assertThat(emittedEvent).isNotNull();
        assertThat(emittedEvent.getEventId()).isNotBlank();
        assertThat(emittedEvent.getPostId()).isEqualTo(postId);
        assertThat(emittedEvent.getUserId()).isEqualTo(userId);

        // --- Step 2: Feed the exact emitted event into Culinary listener ---
        postDeletedSessionListener.listenPostDeletedDelivery(emittedEvent);
        verify(cookingSessionService).markLinkedPostDeleted(postId);
        verify(culinaryIdempotencyService, never()).removeProcessed(anyString(), anyString());

        // --- Step 3: Feed the exact emitted event into Identity listener ---
        postDeleteListener.listenPostDeletedDelivery(emittedEvent);
        verify(statisticsService).incrementCounter(userId, "totalRecipesPublished", -1);
        verify(identityIdempotencyService, never()).removeProcessed(anyString(), anyString());

        // --- Step 4: Feed the exact emitted event into Notification listener ---
        bellNotificationListener.listenPostDeletedDelivery(emittedEvent);
        verify(notificationService).cleanupDeletedPostNotifications(postId);
        verify(notificationIdempotencyService, never()).removeProcessed(anyString(), anyString());
    }

    @Test
    @DisplayName("Duplicate PostDeletedEvent is safely skipped across all three module listeners without side effects")
    void duplicatePostDeletedEvent_SkippedAcrossAllListeners() {
        PostDeletedEvent event = PostDeletedEvent.builder()
                .userId("user-1")
                .postId("post-1")
                .build();

        // All idempotency checks return false (already processed)
        when(culinaryIdempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:culinary-sessions"))
                .thenReturn(false);
        when(identityIdempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:identity"))
                .thenReturn(false);
        when(notificationIdempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:notification"))
                .thenReturn(false);

        postDeletedSessionListener.listenPostDeletedDelivery(event);
        postDeleteListener.listenPostDeletedDelivery(event);
        bellNotificationListener.listenPostDeletedDelivery(event);

        verify(cookingSessionService, never()).markLinkedPostDeleted(anyString());
        verify(statisticsService, never()).incrementCounter(anyString(), anyString(), anyInt());
        verify(notificationService, never()).cleanupDeletedPostNotifications(anyString());
    }

    @Test
    @DisplayName("Transient listener failures release idempotency reservation for Kafka retry across all three modules")
    void transientListenerFailures_ReleaseIdempotencyReservation() {
        PostDeletedEvent event = PostDeletedEvent.builder()
                .userId("user-1")
                .postId("post-1")
                .build();

        // Culinary failure releases culinary idempotency
        when(culinaryIdempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:culinary-sessions"))
                .thenReturn(true);
        doThrow(new RuntimeException("DB unavailable"))
                .when(cookingSessionService).markLinkedPostDeleted(event.getPostId());

        assertThatThrownBy(() -> postDeletedSessionListener.listenPostDeletedDelivery(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB unavailable");
        verify(culinaryIdempotencyService).removeProcessed(event.getEventId(), "post-deleted-delivery:culinary-sessions");

        // Identity failure releases identity idempotency
        when(identityIdempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:identity"))
                .thenReturn(true);
        doThrow(new RuntimeException("Redis error"))
                .when(statisticsService).incrementCounter(event.getUserId(), "totalRecipesPublished", -1);

        assertThatThrownBy(() -> postDeleteListener.listenPostDeletedDelivery(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Redis error");
        verify(identityIdempotencyService).removeProcessed(event.getEventId(), "post-deleted-delivery:identity");

        // Notification failure releases notification idempotency
        when(notificationIdempotencyService.tryProcess(event.getEventId(), "post-deleted-delivery:notification"))
                .thenReturn(true);
        doThrow(new RuntimeException("Notification write error"))
                .when(notificationService).cleanupDeletedPostNotifications(event.getPostId());

        assertThatThrownBy(() -> bellNotificationListener.listenPostDeletedDelivery(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Notification write error");
        verify(notificationIdempotencyService).removeProcessed(event.getEventId(), "post-deleted-delivery:notification");
    }

    @Test
    @DisplayName("Malformed PostDeletedEvent envelopes fail closed without mutations or exceptions")
    void malformedEvents_FailClosedWithoutMutations() {
        // Null event
        postDeletedSessionListener.listenPostDeletedDelivery(null);
        postDeleteListener.listenPostDeletedDelivery(null);
        bellNotificationListener.listenPostDeletedDelivery(null);

        // Missing eventId
        PostDeletedEvent missingEventId = new PostDeletedEvent("user-1", "post-1");
        missingEventId.setEventId(null);

        postDeletedSessionListener.listenPostDeletedDelivery(missingEventId);
        postDeleteListener.listenPostDeletedDelivery(missingEventId);
        bellNotificationListener.listenPostDeletedDelivery(missingEventId);

        // Blank postId (culinary and notification)
        PostDeletedEvent blankPostId = PostDeletedEvent.builder()
                .userId("user-1")
                .postId("   ")
                .build();

        postDeletedSessionListener.listenPostDeletedDelivery(blankPostId);
        bellNotificationListener.listenPostDeletedDelivery(blankPostId);

        // Blank userId (identity)
        PostDeletedEvent blankUserId = PostDeletedEvent.builder()
                .userId("   ")
                .postId("post-1")
                .build();

        postDeleteListener.listenPostDeletedDelivery(blankUserId);

        verify(cookingSessionService, never()).markLinkedPostDeleted(anyString());
        verify(statisticsService, never()).incrementCounter(anyString(), anyString(), anyInt());
        verify(notificationService, never()).cleanupDeletedPostNotifications(anyString());
    }
}
