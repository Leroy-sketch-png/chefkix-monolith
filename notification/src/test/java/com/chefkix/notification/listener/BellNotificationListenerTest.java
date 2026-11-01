package com.chefkix.notification.listener;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.notification.service.NotificationService;
import com.chefkix.shared.event.*;
import com.chefkix.shared.service.KafkaIdempotencyService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BellNotificationListenerTest {

    private static final String GAMIFICATION_SCOPE = "gamification-delivery";
    private static final String POST_LIKED_SCOPE = "post-liked-delivery";
    private static final String NEW_FOLLOWER_SCOPE = "new-follower-delivery";
    private static final String COMMENT_SCOPE = "comment-delivery";
    private static final String REMINDER_SCOPE = "reminder-delivery";
    private static final String TAG_SCOPE = "tag-delivery";
    private static final String STORY_SCOPE = "story-delivery";
    private static final String GROUP_SCOPE = "group-delivery";

    @Mock
    private NotificationService notificationService;
    @Mock
    private KafkaIdempotencyService idempotencyService;

    private BellNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new BellNotificationListener(notificationService, idempotencyService);
    }

    // ==========================================
    // 1. Universal shouldProcess Guard Checks
    // ==========================================

    @Test
    void shouldProcessSkipsNullEvent() {
        listener.listenPostLikedDelivery(null);

        verify(idempotencyService, never()).tryProcess(any(), any());
        verify(notificationService, never()).handlePostLikeEvent(any());
    }

    @Test
    void shouldProcessSkipsEventWithMissingOrBlankEventId() {
        PostLikeEvent event = PostLikeEvent.builder()
                .postId("post-1")
                .likerId("user-liker")
                .postOwnerId("user-owner")
                .build();
        event.setEventId("   ");

        listener.listenPostLikedDelivery(event);

        verify(idempotencyService, never()).tryProcess(anyString(), anyString());
        verify(notificationService, never()).handlePostLikeEvent(any());
    }

    // ==========================================
    // 2. Gamification Delivery
    // ==========================================

    @Test
    void listenGamificationDeliverySkipsDuplicateEvent() {
        GamificationNotificationEvent event = GamificationNotificationEvent.builder()
                .userId("user-1")
                .xpEarned(70.0)
                .source("LINKING_POST")
                .sessionId("session-1")
                .recipeId("recipe-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), GAMIFICATION_SCOPE)).thenReturn(false);

        listener.listenGamificationDelivery(event);

        verify(notificationService, never()).handleGamificationEvent(event);
        verify(idempotencyService, never()).removeProcessed(event.getEventId(), GAMIFICATION_SCOPE);
    }

    @Test
    void listenGamificationDeliverySkipsInvalidBlankUserId() {
        GamificationNotificationEvent event = GamificationNotificationEvent.builder()
                .userId("   ")
                .xpEarned(70.0)
                .source("LINKING_POST")
                .sessionId("session-1")
                .recipeId("recipe-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), GAMIFICATION_SCOPE)).thenReturn(true);

        listener.listenGamificationDelivery(event);

        verify(notificationService, never()).handleGamificationEvent(event);
        verify(idempotencyService, never()).removeProcessed(event.getEventId(), GAMIFICATION_SCOPE);
    }

    @Test
    void listenGamificationDeliveryRoutesValidEvent() {
        GamificationNotificationEvent event = GamificationNotificationEvent.builder()
                .userId("user-1")
                .xpEarned(70.0)
                .source("LINKING_POST")
                .sessionId("session-1")
                .recipeId("recipe-1")
                .newBadges(List.of())
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), GAMIFICATION_SCOPE)).thenReturn(true);

        listener.listenGamificationDelivery(event);

        verify(notificationService).handleGamificationEvent(event);
        verify(idempotencyService, never()).removeProcessed(event.getEventId(), GAMIFICATION_SCOPE);
    }

    @Test
    void listenGamificationDeliveryReleasesIdempotencyOnFailure() {
        GamificationNotificationEvent event = GamificationNotificationEvent.builder()
                .userId("user-1")
                .xpEarned(70.0)
                .source("LINKING_POST")
                .sessionId("session-1")
                .recipeId("recipe-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), GAMIFICATION_SCOPE)).thenReturn(true);
        RuntimeException failure = new RuntimeException("notification save failed");
        doThrow(failure).when(notificationService).handleGamificationEvent(event);

        assertThrows(RuntimeException.class, () -> listener.listenGamificationDelivery(event));

        verify(idempotencyService).removeProcessed(event.getEventId(), GAMIFICATION_SCOPE);
    }

    // ==========================================
    // 3. Post Liked Delivery
    // ==========================================

    @Test
    void listenPostLikedDeliverySkipsDuplicateEvent() {
        PostLikeEvent event = PostLikeEvent.builder()
                .postId("post-1")
                .likerId("user-liker")
                .postOwnerId("user-owner")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), POST_LIKED_SCOPE)).thenReturn(false);

        listener.listenPostLikedDelivery(event);

        verify(notificationService, never()).handlePostLikeEvent(any());
        verify(idempotencyService, never()).removeProcessed(anyString(), anyString());
    }

    @Test
    void listenPostLikedDeliverySkipsInvalidBlankFields() {
        // Blank postId
        PostLikeEvent blankPost = PostLikeEvent.builder()
                .postId("   ")
                .likerId("user-liker")
                .postOwnerId("user-owner")
                .build();
        when(idempotencyService.tryProcess(blankPost.getEventId(), POST_LIKED_SCOPE)).thenReturn(true);
        listener.listenPostLikedDelivery(blankPost);

        // Blank likerId
        PostLikeEvent blankLiker = PostLikeEvent.builder()
                .postId("post-1")
                .likerId("   ")
                .postOwnerId("user-owner")
                .build();
        when(idempotencyService.tryProcess(blankLiker.getEventId(), POST_LIKED_SCOPE)).thenReturn(true);
        listener.listenPostLikedDelivery(blankLiker);

        // Blank postOwnerId
        PostLikeEvent blankOwner = PostLikeEvent.builder()
                .postId("post-1")
                .likerId("user-liker")
                .postOwnerId("   ")
                .build();
        when(idempotencyService.tryProcess(blankOwner.getEventId(), POST_LIKED_SCOPE)).thenReturn(true);
        listener.listenPostLikedDelivery(blankOwner);

        verify(notificationService, never()).handlePostLikeEvent(any());
        verify(idempotencyService, never()).removeProcessed(anyString(), anyString());
    }

    @Test
    void listenPostLikedDeliveryRoutesValidEvent() {
        PostLikeEvent event = PostLikeEvent.builder()
                .postId("post-1")
                .likerId("user-liker")
                .postOwnerId("user-owner")
                .displayName("Liker")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), POST_LIKED_SCOPE)).thenReturn(true);

        listener.listenPostLikedDelivery(event);

        verify(notificationService).handlePostLikeEvent(event);
        verify(idempotencyService, never()).removeProcessed(anyString(), anyString());
    }

    @Test
    void listenPostLikedDeliveryReleasesIdempotencyOnFailure() {
        PostLikeEvent event = PostLikeEvent.builder()
                .postId("post-1")
                .likerId("user-liker")
                .postOwnerId("user-owner")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), POST_LIKED_SCOPE)).thenReturn(true);
        doThrow(new RuntimeException("database timeout")).when(notificationService).handlePostLikeEvent(event);

        assertThrows(RuntimeException.class, () -> listener.listenPostLikedDelivery(event));

        verify(idempotencyService).removeProcessed(event.getEventId(), POST_LIKED_SCOPE);
    }

    @Test
    void listenPostLikedDeliverySkipsUnexpectedEventType() {
        NewFollowerEvent wrongEvent = NewFollowerEvent.builder()
                .followerId("user-follower")
                .followedUserId("user-owner")
                .build();

        when(idempotencyService.tryProcess(wrongEvent.getEventId(), POST_LIKED_SCOPE)).thenReturn(true);

        listener.listenPostLikedDelivery(wrongEvent);

        verify(notificationService, never()).handlePostLikeEvent(any());
        verify(idempotencyService, never()).removeProcessed(anyString(), anyString());
    }

    // ==========================================
    // 4. New Follower Delivery
    // ==========================================

    @Test
    void listenNewFollowerDeliverySkipsDuplicateEvent() {
        NewFollowerEvent event = NewFollowerEvent.builder()
                .followerId("user-f")
                .followedUserId("user-t")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), NEW_FOLLOWER_SCOPE)).thenReturn(false);

        listener.listenNewFollowerDelivery(event);

        verify(notificationService, never()).handleNewFollowerEvent(any());
    }

    @Test
    void listenNewFollowerDeliverySkipsInvalidBlankFields() {
        NewFollowerEvent blankFollower = NewFollowerEvent.builder()
                .followerId("   ")
                .followedUserId("user-t")
                .build();
        when(idempotencyService.tryProcess(blankFollower.getEventId(), NEW_FOLLOWER_SCOPE)).thenReturn(true);
        listener.listenNewFollowerDelivery(blankFollower);

        NewFollowerEvent blankFollowed = NewFollowerEvent.builder()
                .followerId("user-f")
                .followedUserId("   ")
                .build();
        when(idempotencyService.tryProcess(blankFollowed.getEventId(), NEW_FOLLOWER_SCOPE)).thenReturn(true);
        listener.listenNewFollowerDelivery(blankFollowed);

        verify(notificationService, never()).handleNewFollowerEvent(any());
    }

    @Test
    void listenNewFollowerDeliveryRoutesValidEvent() {
        NewFollowerEvent event = NewFollowerEvent.builder()
                .followerId("user-f")
                .followedUserId("user-t")
                .followerDisplayName("Follower")
                .isMutualFollow(true)
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), NEW_FOLLOWER_SCOPE)).thenReturn(true);

        listener.listenNewFollowerDelivery(event);

        verify(notificationService).handleNewFollowerEvent(event);
        verify(idempotencyService, never()).removeProcessed(anyString(), anyString());
    }

    @Test
    void listenNewFollowerDeliveryReleasesIdempotencyOnFailure() {
        NewFollowerEvent event = NewFollowerEvent.builder()
                .followerId("user-f")
                .followedUserId("user-t")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), NEW_FOLLOWER_SCOPE)).thenReturn(true);
        doThrow(new RuntimeException("save failed")).when(notificationService).handleNewFollowerEvent(event);

        assertThrows(RuntimeException.class, () -> listener.listenNewFollowerDelivery(event));

        verify(idempotencyService).removeProcessed(event.getEventId(), NEW_FOLLOWER_SCOPE);
    }

    // ==========================================
    // 5. Comment Delivery
    // ==========================================

    @Test
    void listenCommentDeliverySkipsDuplicateEvent() {
        CommentEvent event = CommentEvent.builder()
                .postId("post-1")
                .commentId("c-1")
                .commenterId("user-c")
                .postOwnerId("user-o")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), COMMENT_SCOPE)).thenReturn(false);

        listener.listenCommentDelivery(event);

        verify(notificationService, never()).handleCommentEvent(any());
    }

    @Test
    void listenCommentDeliverySkipsInvalidBlankFields() {
        CommentEvent blankPost = CommentEvent.builder()
                .postId("   ")
                .postOwnerId("user-o")
                .commenterId("user-c")
                .build();
        when(idempotencyService.tryProcess(blankPost.getEventId(), COMMENT_SCOPE)).thenReturn(true);
        listener.listenCommentDelivery(blankPost);

        CommentEvent blankOwner = CommentEvent.builder()
                .postId("post-1")
                .postOwnerId("   ")
                .commenterId("user-c")
                .build();
        when(idempotencyService.tryProcess(blankOwner.getEventId(), COMMENT_SCOPE)).thenReturn(true);
        listener.listenCommentDelivery(blankOwner);

        CommentEvent blankCommenter = CommentEvent.builder()
                .postId("post-1")
                .postOwnerId("user-o")
                .commenterId("   ")
                .build();
        when(idempotencyService.tryProcess(blankCommenter.getEventId(), COMMENT_SCOPE)).thenReturn(true);
        listener.listenCommentDelivery(blankCommenter);

        verify(notificationService, never()).handleCommentEvent(any());
    }

    @Test
    void listenCommentDeliveryRoutesValidEvent() {
        CommentEvent event = CommentEvent.builder()
                .postId("post-1")
                .commentId("c-1")
                .commenterId("user-c")
                .postOwnerId("user-o")
                .contentPreview("Delicious!")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), COMMENT_SCOPE)).thenReturn(true);

        listener.listenCommentDelivery(event);

        verify(notificationService).handleCommentEvent(event);
        verify(idempotencyService, never()).removeProcessed(anyString(), anyString());
    }

    @Test
    void listenCommentDeliveryReleasesIdempotencyOnFailure() {
        CommentEvent event = CommentEvent.builder()
                .postId("post-1")
                .commentId("c-1")
                .commenterId("user-c")
                .postOwnerId("user-o")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), COMMENT_SCOPE)).thenReturn(true);
        doThrow(new RuntimeException("comment notification error")).when(notificationService).handleCommentEvent(event);

        assertThrows(RuntimeException.class, () -> listener.listenCommentDelivery(event));

        verify(idempotencyService).removeProcessed(event.getEventId(), COMMENT_SCOPE);
    }

    // ==========================================
    // 6. Reminder Delivery (ReminderEvent & DuelEvent)
    // ==========================================

    @Test
    void listenReminderDeliverySkipsDuplicateEvent() {
        ReminderEvent event = ReminderEvent.builder()
                .userId("user-1")
                .reminderType("DAILY_STREAK")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), REMINDER_SCOPE)).thenReturn(false);

        listener.listenReminderDelivery(event);

        verify(notificationService, never()).handleReminderEvent(any());
        verify(notificationService, never()).handleDuelEvent(any());
    }

    @Test
    void listenReminderDeliverySkipsInvalidBlankReminderFields() {
        ReminderEvent blankUser = ReminderEvent.builder()
                .userId("   ")
                .reminderType("DAILY_STREAK")
                .build();
        when(idempotencyService.tryProcess(blankUser.getEventId(), REMINDER_SCOPE)).thenReturn(true);
        listener.listenReminderDelivery(blankUser);

        ReminderEvent blankType = ReminderEvent.builder()
                .userId("user-1")
                .reminderType("   ")
                .build();
        when(idempotencyService.tryProcess(blankType.getEventId(), REMINDER_SCOPE)).thenReturn(true);
        listener.listenReminderDelivery(blankType);

        verify(notificationService, never()).handleReminderEvent(any());
    }

    @Test
    void listenReminderDeliveryRoutesValidReminderEvent() {
        ReminderEvent event = ReminderEvent.builder()
                .userId("user-1")
                .reminderType("DAILY_STREAK")
                .content("Keep cooking!")
                .priority(ReminderEvent.ReminderPriority.NORMAL)
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), REMINDER_SCOPE)).thenReturn(true);

        listener.listenReminderDelivery(event);

        verify(notificationService).handleReminderEvent(event);
        verify(idempotencyService, never()).removeProcessed(anyString(), anyString());
    }

    @Test
    void listenReminderDeliveryReleasesIdempotencyOnReminderFailure() {
        ReminderEvent event = ReminderEvent.builder()
                .userId("user-1")
                .reminderType("DAILY_STREAK")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), REMINDER_SCOPE)).thenReturn(true);
        doThrow(new RuntimeException("reminder failed")).when(notificationService).handleReminderEvent(event);

        assertThrows(RuntimeException.class, () -> listener.listenReminderDelivery(event));

        verify(idempotencyService).removeProcessed(event.getEventId(), REMINDER_SCOPE);
    }

    @Test
    void listenReminderDeliverySkipsInvalidBlankDuelFields() {
        DuelEvent blankUser = DuelEvent.builder()
                .userId("   ")
                .duelId("duel-1")
                .build();
        when(idempotencyService.tryProcess(blankUser.getEventId(), REMINDER_SCOPE)).thenReturn(true);
        listener.listenReminderDelivery(blankUser);

        DuelEvent blankDuel = DuelEvent.builder()
                .userId("user-1")
                .duelId("   ")
                .build();
        when(idempotencyService.tryProcess(blankDuel.getEventId(), REMINDER_SCOPE)).thenReturn(true);
        listener.listenReminderDelivery(blankDuel);

        verify(notificationService, never()).handleDuelEvent(any());
    }

    @Test
    void listenReminderDeliveryRoutesValidDuelEvent() {
        DuelEvent event = DuelEvent.builder()
                .userId("user-1")
                .duelId("duel-1")
                .duelAction("DUEL_INVITE")
                .challengerName("Chef Mario")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), REMINDER_SCOPE)).thenReturn(true);

        listener.listenReminderDelivery(event);

        verify(notificationService).handleDuelEvent(event);
        verify(idempotencyService, never()).removeProcessed(anyString(), anyString());
    }

    @Test
    void listenReminderDeliveryReleasesIdempotencyOnDuelFailure() {
        DuelEvent event = DuelEvent.builder()
                .userId("user-1")
                .duelId("duel-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), REMINDER_SCOPE)).thenReturn(true);
        doThrow(new RuntimeException("duel failed")).when(notificationService).handleDuelEvent(event);

        assertThrows(RuntimeException.class, () -> listener.listenReminderDelivery(event));

        verify(idempotencyService).removeProcessed(event.getEventId(), REMINDER_SCOPE);
    }

    @Test
    void listenReminderDeliverySkipsUnexpectedEventType() {
        CommentEvent wrongEvent = CommentEvent.builder()
                .postId("post-1")
                .postOwnerId("user-o")
                .commenterId("user-c")
                .build();

        when(idempotencyService.tryProcess(wrongEvent.getEventId(), REMINDER_SCOPE)).thenReturn(true);

        listener.listenReminderDelivery(wrongEvent);

        verify(notificationService, never()).handleReminderEvent(any());
        verify(notificationService, never()).handleDuelEvent(any());
    }

    // ==========================================
    // 7. Tag Delivery (UserMentionEvent)
    // ==========================================

    @Test
    void listenTagDeliverySkipsDuplicateEvent() {
        UserMentionEvent event = UserMentionEvent.builder()
                .recipientId("user-1")
                .sourceId("s-1")
                .sourceType("COMMENT")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), TAG_SCOPE)).thenReturn(false);

        listener.listenTagDelivery(event);

        verify(notificationService, never()).handleTagEvent(any());
    }

    @Test
    void listenTagDeliverySkipsInvalidBlankFields() {
        UserMentionEvent blankUser = UserMentionEvent.builder()
                .recipientId("   ")
                .sourceType("COMMENT")
                .build();
        when(idempotencyService.tryProcess(blankUser.getEventId(), TAG_SCOPE)).thenReturn(true);
        listener.listenTagDelivery(blankUser);

        UserMentionEvent blankSource = UserMentionEvent.builder()
                .recipientId("user-1")
                .sourceType("   ")
                .build();
        when(idempotencyService.tryProcess(blankSource.getEventId(), TAG_SCOPE)).thenReturn(true);
        listener.listenTagDelivery(blankSource);

        verify(notificationService, never()).handleTagEvent(any());
    }

    @Test
    void listenTagDeliveryRoutesValidEvent() {
        UserMentionEvent event = UserMentionEvent.builder()
                .recipientId("user-1")
                .sourceId("s-1")
                .sourceType("COMMENT")
                .postId("post-1")
                .actorDisplayName("Chef Luigi")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), TAG_SCOPE)).thenReturn(true);

        listener.listenTagDelivery(event);

        verify(notificationService).handleTagEvent(event);
        verify(idempotencyService, never()).removeProcessed(anyString(), anyString());
    }

    @Test
    void listenTagDeliveryReleasesIdempotencyOnFailure() {
        UserMentionEvent event = UserMentionEvent.builder()
                .recipientId("user-1")
                .sourceId("s-1")
                .sourceType("COMMENT")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), TAG_SCOPE)).thenReturn(true);
        doThrow(new RuntimeException("tag error")).when(notificationService).handleTagEvent(event);

        assertThrows(RuntimeException.class, () -> listener.listenTagDelivery(event));

        verify(idempotencyService).removeProcessed(event.getEventId(), TAG_SCOPE);
    }

    // ==========================================
    // 8. Story Delivery (StoryInteractionEvent)
    // ==========================================

    @Test
    void listenStoryDeliverySkipsDuplicateEvent() {
        StoryInteractionEvent event = StoryInteractionEvent.builder()
                .storyId("story-1")
                .userId("user-1")
                .interactionType("LIKE")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), STORY_SCOPE)).thenReturn(false);

        listener.listenStoryDelivery(event);

        verify(notificationService, never()).handleStoryInteractedEvent(any());
    }

    @Test
    void listenStoryDeliverySkipsInvalidBlankFields() {
        StoryInteractionEvent blankStory = StoryInteractionEvent.builder()
                .storyId("   ")
                .userId("user-1")
                .interactionType("LIKE")
                .build();
        when(idempotencyService.tryProcess(blankStory.getEventId(), STORY_SCOPE)).thenReturn(true);
        listener.listenStoryDelivery(blankStory);

        StoryInteractionEvent blankUser = StoryInteractionEvent.builder()
                .storyId("story-1")
                .userId("   ")
                .interactionType("LIKE")
                .build();
        when(idempotencyService.tryProcess(blankUser.getEventId(), STORY_SCOPE)).thenReturn(true);
        listener.listenStoryDelivery(blankUser);

        StoryInteractionEvent blankType = StoryInteractionEvent.builder()
                .storyId("story-1")
                .userId("user-1")
                .interactionType("   ")
                .build();
        when(idempotencyService.tryProcess(blankType.getEventId(), STORY_SCOPE)).thenReturn(true);
        listener.listenStoryDelivery(blankType);

        verify(notificationService, never()).handleStoryInteractedEvent(any());
    }

    @Test
    void listenStoryDeliveryRoutesValidEvent() {
        StoryInteractionEvent event = StoryInteractionEvent.builder()
                .storyId("story-1")
                .userId("user-1")
                .storyOwnerId("owner-1")
                .userDisplayName("Chef Toad")
                .interactionType("REACT_FIRE")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), STORY_SCOPE)).thenReturn(true);

        listener.listenStoryDelivery(event);

        verify(notificationService).handleStoryInteractedEvent(event);
        verify(idempotencyService, never()).removeProcessed(anyString(), anyString());
    }

    @Test
    void listenStoryDeliveryReleasesIdempotencyOnFailure() {
        StoryInteractionEvent event = StoryInteractionEvent.builder()
                .storyId("story-1")
                .userId("user-1")
                .interactionType("LIKE")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), STORY_SCOPE)).thenReturn(true);
        doThrow(new RuntimeException("story error")).when(notificationService).handleStoryInteractedEvent(event);

        assertThrows(RuntimeException.class, () -> listener.listenStoryDelivery(event));

        verify(idempotencyService).removeProcessed(event.getEventId(), STORY_SCOPE);
    }

    // ==========================================
    // 9. Group Delivery (3 Event Types)
    // ==========================================

    @Test
    void listenGroupDeliverySkipsDuplicateEvent() {
        GroupJoinRequestedEvent event = GroupJoinRequestedEvent.builder()
                .groupId("group-1")
                .requesterId("user-1")
                .build();

        when(idempotencyService.tryProcess(event.getEventId(), GROUP_SCOPE)).thenReturn(false);

        listener.listenGroupDelivery(event);

        verify(notificationService, never()).handleGroupJoinRequestedEvent(any());
        verify(notificationService, never()).handleGroupMemberJoinedEvent(any());
        verify(notificationService, never()).handleGroupRequestApprovedEvent(any());
    }

    @Test
    void listenGroupDeliveryHandlesJoinRequested() {
        // Blank fields skip
        GroupJoinRequestedEvent blankGroup = GroupJoinRequestedEvent.builder()
                .groupId("   ")
                .requesterId("user-1")
                .build();
        when(idempotencyService.tryProcess(blankGroup.getEventId(), GROUP_SCOPE)).thenReturn(true);
        listener.listenGroupDelivery(blankGroup);

        GroupJoinRequestedEvent blankReq = GroupJoinRequestedEvent.builder()
                .groupId("group-1")
                .requesterId("   ")
                .build();
        when(idempotencyService.tryProcess(blankReq.getEventId(), GROUP_SCOPE)).thenReturn(true);
        listener.listenGroupDelivery(blankReq);

        verify(notificationService, never()).handleGroupJoinRequestedEvent(any());

        // Valid routes
        GroupJoinRequestedEvent valid = GroupJoinRequestedEvent.builder()
                .groupId("group-1")
                .groupName("Sous Chefs")
                .requesterId("user-1")
                .adminId("admin-1")
                .build();
        when(idempotencyService.tryProcess(valid.getEventId(), GROUP_SCOPE)).thenReturn(true);
        listener.listenGroupDelivery(valid);
        verify(notificationService).handleGroupJoinRequestedEvent(valid);

        // Failure releases idempotency
        doThrow(new RuntimeException("join request error")).when(notificationService).handleGroupJoinRequestedEvent(valid);
        assertThrows(RuntimeException.class, () -> listener.listenGroupDelivery(valid));
        verify(idempotencyService).removeProcessed(valid.getEventId(), GROUP_SCOPE);
    }

    @Test
    void listenGroupDeliveryHandlesMemberJoined() {
        // Blank fields skip
        GroupMemberJoinedEvent blankGroup = GroupMemberJoinedEvent.builder()
                .groupId("   ")
                .memberId("user-1")
                .build();
        when(idempotencyService.tryProcess(blankGroup.getEventId(), GROUP_SCOPE)).thenReturn(true);
        listener.listenGroupDelivery(blankGroup);

        GroupMemberJoinedEvent blankMember = GroupMemberJoinedEvent.builder()
                .groupId("group-1")
                .memberId("   ")
                .build();
        when(idempotencyService.tryProcess(blankMember.getEventId(), GROUP_SCOPE)).thenReturn(true);
        listener.listenGroupDelivery(blankMember);

        verify(notificationService, never()).handleGroupMemberJoinedEvent(any());

        // Valid routes
        GroupMemberJoinedEvent valid = GroupMemberJoinedEvent.builder()
                .groupId("group-1")
                .groupName("Sous Chefs")
                .memberId("user-1")
                .adminId("admin-1")
                .build();
        when(idempotencyService.tryProcess(valid.getEventId(), GROUP_SCOPE)).thenReturn(true);
        listener.listenGroupDelivery(valid);
        verify(notificationService).handleGroupMemberJoinedEvent(valid);

        // Failure releases idempotency
        doThrow(new RuntimeException("join error")).when(notificationService).handleGroupMemberJoinedEvent(valid);
        assertThrows(RuntimeException.class, () -> listener.listenGroupDelivery(valid));
        verify(idempotencyService).removeProcessed(valid.getEventId(), GROUP_SCOPE);
    }

    @Test
    void listenGroupDeliveryHandlesRequestApproved() {
        // Blank fields skip
        GroupRequestApprovedEvent blankGroup = GroupRequestApprovedEvent.builder()
                .groupId("   ")
                .requesterId("user-1")
                .build();
        when(idempotencyService.tryProcess(blankGroup.getEventId(), GROUP_SCOPE)).thenReturn(true);
        listener.listenGroupDelivery(blankGroup);

        GroupRequestApprovedEvent blankReq = GroupRequestApprovedEvent.builder()
                .groupId("group-1")
                .requesterId("   ")
                .build();
        when(idempotencyService.tryProcess(blankReq.getEventId(), GROUP_SCOPE)).thenReturn(true);
        listener.listenGroupDelivery(blankReq);

        verify(notificationService, never()).handleGroupRequestApprovedEvent(any());

        // Valid routes
        GroupRequestApprovedEvent valid = GroupRequestApprovedEvent.builder()
                .groupId("group-1")
                .groupName("Sous Chefs")
                .requesterId("user-1")
                .adminId("admin-1")
                .build();
        when(idempotencyService.tryProcess(valid.getEventId(), GROUP_SCOPE)).thenReturn(true);
        listener.listenGroupDelivery(valid);
        verify(notificationService).handleGroupRequestApprovedEvent(valid);

        // Failure releases idempotency
        doThrow(new RuntimeException("approve error")).when(notificationService).handleGroupRequestApprovedEvent(valid);
        assertThrows(RuntimeException.class, () -> listener.listenGroupDelivery(valid));
        verify(idempotencyService).removeProcessed(valid.getEventId(), GROUP_SCOPE);
    }

    @Test
    void listenGroupDeliverySkipsUnexpectedEventType() {
        CommentEvent wrongEvent = CommentEvent.builder()
                .postId("post-1")
                .postOwnerId("user-o")
                .commenterId("user-c")
                .build();

        when(idempotencyService.tryProcess(wrongEvent.getEventId(), GROUP_SCOPE)).thenReturn(true);

        listener.listenGroupDelivery(wrongEvent);

        verify(notificationService, never()).handleGroupJoinRequestedEvent(any());
        verify(notificationService, never()).handleGroupMemberJoinedEvent(any());
        verify(notificationService, never()).handleGroupRequestApprovedEvent(any());
    }
}
