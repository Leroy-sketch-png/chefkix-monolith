package com.chefkix.social.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.social.post.entity.Comment;
import com.chefkix.social.post.entity.CommentLike;
import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.entity.PostLike;
import com.chefkix.social.post.entity.PostSave;
import com.chefkix.social.post.entity.PollData;
import com.chefkix.social.post.entity.PollVote;
import com.chefkix.social.post.entity.PlateRating;
import com.chefkix.social.post.entity.Reply;
import com.chefkix.social.post.entity.ReplyLike;
import com.chefkix.social.post.entity.BattleVote;
import com.chefkix.social.post.events.PostIndexEvent;
import com.chefkix.social.post.enums.PostStatus;
import com.chefkix.social.post.repository.BattleVoteRepository;
import com.chefkix.social.post.repository.CollectionProgressRepository;
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
import com.chefkix.social.chat.entity.ChatMessage;
import com.chefkix.social.chat.entity.Conversation;
import com.chefkix.social.chat.entity.ParticipantInfo;
import com.chefkix.social.chat.repository.ChatMessageRepository;
import com.chefkix.social.chat.repository.ConversationRepository;
import com.chefkix.social.group.service.GroupService;
import com.chefkix.social.story.entity.Story;
import com.chefkix.social.story.entity.StoryHighlight;
import com.chefkix.social.story.entity.StoryInteraction;
import com.chefkix.social.story.repository.StoryHighlightRepository;
import com.chefkix.social.story.repository.StoryInteractionRepository;
import com.chefkix.social.story.repository.StoryRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PostProviderImplTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ReplyRepository replyRepository;
        @Mock
        private PostLikeRepository postLikeRepository;
        @Mock
        private PostSaveRepository postSaveRepository;
        @Mock
        private PollVoteRepository pollVoteRepository;
        @Mock
        private BattleVoteRepository battleVoteRepository;
        @Mock
        private PlateRatingRepository plateRatingRepository;
        @Mock
        private CommentLikeRepository commentLikeRepository;
        @Mock
        private ReplyLikeRepository replyLikeRepository;
    @Mock
    private CollectionRepository collectionRepository;
    @Mock
    private CollectionProgressRepository collectionProgressRepository;
    @Mock
    private StoryRepository storyRepository;
    @Mock
    private StoryInteractionRepository storyInteractionRepository;
    @Mock
    private StoryHighlightRepository storyHighlightRepository;
        @Mock
        private ConversationRepository conversationRepository;
        @Mock
        private ChatMessageRepository chatMessageRepository;
        @Mock
        private GroupService groupService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PostProviderImpl provider;

    @BeforeEach
    void setUp() {
        provider = new PostProviderImpl(
                                null,
                postRepository,
                commentRepository,
                replyRepository,
                                postLikeRepository,
                                postSaveRepository,
                                pollVoteRepository,
                                battleVoteRepository,
                                plateRatingRepository,
                                commentLikeRepository,
                                replyLikeRepository,
                collectionRepository,
                collectionProgressRepository,
                storyRepository,
                storyInteractionRepository,
                storyHighlightRepository,
                conversationRepository,
                chatMessageRepository,
                groupService,
                eventPublisher);
    }

    @Test
        void cleanupDeletedUserDataAnonymizesOwnedContentDeletesInteractionResidueAndTombstonesChatResidue() {
        String userId = "user-1";

        Post post = Post.builder()
                .id("post-1")
                .displayName("Alice")
                .avatarUrl("avatar.png")
                .verified(true)
                .content("Original post")
                .photoUrls(new ArrayList<>(List.of("plate.jpg")))
                .videoUrl("video.mp4")
                .slug("original-post")
                .postUrl("/posts/original-post")
                .roomCode("ROOM123")
                .tags(new ArrayList<>(List.of("tag")))
                .taggedUserIds(new ArrayList<>(List.of("friend-1")))
                .likes(3)
                .battleVotesA(2)
                .battleVotesB(1)
                .fireCount(1)
                .cringeCount(1)
                .pollData(PollData.builder().votesA(4).votesB(2).build())
                .status(PostStatus.ACTIVE)
                .hidden(false)
                .build();
        Comment comment = Comment.builder()
                .id("comment-1")
                .displayName("Alice")
                .avatarUrl("avatar.png")
                .content("Original comment")
                .likes(2)
                .taggedUserIds(new ArrayList<>(List.of("friend-1")))
                .build();
        Reply reply = Reply.builder()
                .id("reply-1")
                .displayName("Alice")
                .avatarUrl("avatar.png")
                .content("Original reply")
                .likes(1)
                .taggedUserIds(new ArrayList<>(List.of("friend-1")))
                .build();
        Story story = Story.builder()
                .id("story-1")
                .userId(userId)
                .mediaUrl("story.jpg")
                .items(new ArrayList<>())
                .recipeId("recipe-1")
                .isDeleted(false)
                .build();
        Conversation conversation = Conversation.builder()
                .id("conversation-1")
                .participants(new ArrayList<>(List.of(
                        ParticipantInfo.builder()
                                .userId(userId)
                                .username("Alice")
                                .firstName("Alice")
                                .lastName("One")
                                .avatar("avatar.png")
                                .build(),
                        ParticipantInfo.builder()
                                .userId("user-2")
                                .username("Bob")
                                .firstName("Bob")
                                .lastName("Two")
                                .avatar("bob.png")
                                .build())))
                .build();
        ChatMessage authoredMessage = ChatMessage.builder()
                .id("message-1")
                .message("Secret chat")
                .relatedId("post-1")
                .sharedPostImage("shared.png")
                .sharedPostTitle("Shared post")
                .replyToContent("Old reply")
                .sender(ParticipantInfo.builder()
                        .userId(userId)
                        .username("Alice")
                        .firstName("Alice")
                        .lastName("One")
                        .avatar("avatar.png")
                        .build())
                .reactions(new ArrayList<>(List.of(ChatMessage.Reaction.builder()
                        .emoji("🔥")
                        .userIds(new ArrayList<>(List.of("user-3")))
                        .build())))
                .deleted(false)
                .build();
        ChatMessage replyReference = ChatMessage.builder()
                .id("message-2")
                .replyToId("message-1")
                .replyToSenderName("Alice")
                .sender(ParticipantInfo.builder()
                        .userId("user-2")
                        .username("Bob")
                        .firstName("Bob")
                        .lastName("Two")
                        .avatar("bob.png")
                        .build())
                .reactions(new ArrayList<>())
                .build();
        ChatMessage reactedMessage = ChatMessage.builder()
                .id("message-3")
                .sender(ParticipantInfo.builder()
                        .userId("user-2")
                        .username("Bob")
                        .firstName("Bob")
                        .lastName("Two")
                        .avatar("bob.png")
                        .build())
                .reactions(new ArrayList<>(List.of(
                        ChatMessage.Reaction.builder()
                                .emoji("🔥")
                                .userIds(new ArrayList<>(List.of(userId, "user-3")))
                                .build(),
                        ChatMessage.Reaction.builder()
                                .emoji("❤️")
                                .userIds(new ArrayList<>(List.of(userId)))
                                .build())))
                .build();
        PostLike postLike = PostLike.builder().id("post-like-1").postId("post-1").userId(userId).build();
        PostSave postSave = PostSave.builder().id("post-save-1").postId("post-1").userId(userId).build();
        PollVote pollVote = PollVote.builder().id("poll-vote-1").postId("post-1").userId(userId).option("A").build();
        BattleVote battleVote = BattleVote.builder().id("battle-vote-1").postId("post-1").userId(userId).choice("B").build();
        PlateRating plateRating = PlateRating.builder().id("plate-rating-1").postId("post-1").userId(userId).rating("FIRE").build();
        CommentLike commentLike = CommentLike.builder().id("comment-like-1").commentId("comment-1").userId(userId).build();
        ReplyLike replyLike = ReplyLike.builder().id("reply-like-1").replyId("reply-1").userId(userId).build();

        when(postRepository.findAllByUserId(userId)).thenReturn(List.of(post));
        when(postRepository.findById("post-1")).thenReturn(java.util.Optional.of(post));
        when(commentRepository.findAllByUserId(userId)).thenReturn(List.of(comment));
        when(commentRepository.findById("comment-1")).thenReturn(java.util.Optional.of(comment));
        when(replyRepository.findAllByUserId(userId)).thenReturn(List.of(reply));
        when(replyRepository.findById("reply-1")).thenReturn(java.util.Optional.of(reply));
        when(postLikeRepository.findAllByUserId(userId)).thenReturn(List.of(postLike));
        when(postSaveRepository.findAllByUserId(userId)).thenReturn(List.of(postSave));
        when(pollVoteRepository.findAllByUserId(userId)).thenReturn(List.of(pollVote));
        when(battleVoteRepository.findAllByUserId(userId)).thenReturn(List.of(battleVote));
        when(plateRatingRepository.findAllByUserId(userId)).thenReturn(List.of(plateRating));
        when(commentLikeRepository.findAllByUserId(userId)).thenReturn(List.of(commentLike));
        when(replyLikeRepository.findAllByUserId(userId)).thenReturn(List.of(replyLike));
        when(collectionRepository.countByUserId(userId)).thenReturn(2L);
        when(collectionProgressRepository.findAllByUserId(userId))
                .thenReturn(List.of(org.mockito.Mockito.mock(com.chefkix.social.post.entity.CollectionProgress.class)));
        when(storyRepository.findAllByUserId(userId)).thenReturn(List.of(story));
        when(storyInteractionRepository.findAllByUserId(userId)).thenReturn(List.of(
                org.mockito.Mockito.mock(StoryInteraction.class),
                org.mockito.Mockito.mock(StoryInteraction.class)));
        when(storyHighlightRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(org.mockito.Mockito.mock(StoryHighlight.class)));
        when(conversationRepository.findAllByParticipantIdsContains(userId, org.springframework.data.domain.Sort.unsorted()))
                .thenReturn(List.of(conversation));
        when(chatMessageRepository.findAllBySenderUserId(userId)).thenReturn(List.of(authoredMessage));
        when(chatMessageRepository.findAllByReplyToIdIn(List.of("message-1"))).thenReturn(List.of(replyReference));
        when(chatMessageRepository.findAllByReactionUserId(userId)).thenReturn(List.of(reactedMessage));
        when(groupService.cleanupDeletedUserData(userId)).thenReturn(0L);

        long affectedRecords = provider.cleanupDeletedUserData(userId);

        assertThat(affectedRecords).isEqualTo(21);

        assertThat(post.getDisplayName()).isEqualTo("Deleted User");
        assertThat(post.getAvatarUrl()).isNull();
        assertThat(post.isVerified()).isFalse();
        assertThat(post.getContent()).isEqualTo("[deleted]");
        assertThat(post.getPhotoUrls()).isEmpty();
        assertThat(post.getVideoUrl()).isNull();
        assertThat(post.getSlug()).isNull();
        assertThat(post.getPostUrl()).isNull();
        assertThat(post.getRoomCode()).isNull();
        assertThat(post.getTags()).isEmpty();
        assertThat(post.getTaggedUserIds()).isEmpty();
        assertThat(post.isHidden()).isTrue();
        assertThat(post.getStatus()).isEqualTo(PostStatus.DELETED);
        assertThat(post.getLikes()).isEqualTo(2);
        assertThat(post.getPollData().getVotesA()).isEqualTo(3);
        assertThat(post.getPollData().getVotesB()).isEqualTo(2);
        assertThat(post.getBattleVotesA()).isEqualTo(2);
        assertThat(post.getBattleVotesB()).isEqualTo(0);
        assertThat(post.getFireCount()).isEqualTo(0);
        assertThat(post.getCringeCount()).isEqualTo(1);

        assertThat(comment.getDisplayName()).isEqualTo("Deleted User");
        assertThat(comment.getAvatarUrl()).isNull();
        assertThat(comment.getContent()).isEqualTo("[deleted]");
        assertThat(comment.getTaggedUserIds()).isEmpty();
        assertThat(comment.getLikes()).isEqualTo(1);

        assertThat(reply.getDisplayName()).isEqualTo("Deleted User");
        assertThat(reply.getAvatarUrl()).isNull();
        assertThat(reply.getContent()).isEqualTo("[deleted]");
        assertThat(reply.getTaggedUserIds()).isEmpty();
        assertThat(reply.getLikes()).isEqualTo(0);

        assertThat(story.getIsDeleted()).isTrue();
        assertThat(story.getMediaUrl()).isNull();
        assertThat(story.getItems()).isEmpty();
        assertThat(story.getRecipeId()).isNull();

        assertThat(conversation.getParticipants()).hasSize(2);
        assertThat(conversation.getParticipants().get(0).getUsername()).isEqualTo("Deleted User");
        assertThat(conversation.getParticipants().get(0).getFirstName()).isNull();
        assertThat(conversation.getParticipants().get(0).getLastName()).isNull();
        assertThat(conversation.getParticipants().get(0).getAvatar()).isNull();
        assertThat(conversation.getParticipants().get(1).getUsername()).isEqualTo("Bob");

        assertThat(authoredMessage.getDeleted()).isTrue();
        assertThat(authoredMessage.getMessage()).isNull();
        assertThat(authoredMessage.getRelatedId()).isNull();
        assertThat(authoredMessage.getSharedPostImage()).isNull();
        assertThat(authoredMessage.getSharedPostTitle()).isNull();
        assertThat(authoredMessage.getReplyToContent()).isNull();
        assertThat(authoredMessage.getReactions()).isEmpty();
        assertThat(authoredMessage.getSender().getUsername()).isEqualTo("Deleted User");
        assertThat(authoredMessage.getSender().getFirstName()).isNull();
        assertThat(authoredMessage.getSender().getLastName()).isNull();
        assertThat(authoredMessage.getSender().getAvatar()).isNull();

        assertThat(replyReference.getReplyToSenderName()).isEqualTo("Deleted User");

        assertThat(reactedMessage.getReactions()).hasSize(1);
        assertThat(reactedMessage.getReactions().get(0).getEmoji()).isEqualTo("🔥");
        assertThat(reactedMessage.getReactions().get(0).getUserIds()).containsExactly("user-3");

                verify(postRepository).saveAll((Iterable<Post>) List.of(post));
        verify(postRepository, times(4)).save(post);
                verify(commentRepository).saveAll((Iterable<Comment>) List.of(comment));
        verify(commentRepository).save(comment);
                verify(replyRepository).saveAll((Iterable<Reply>) List.of(reply));
        verify(replyRepository).save(reply);
                verify(postLikeRepository).deleteAll((Iterable<PostLike>) List.of(postLike));
                verify(postSaveRepository).deleteAll((Iterable<PostSave>) List.of(postSave));
                verify(pollVoteRepository).deleteAll((Iterable<PollVote>) List.of(pollVote));
                verify(battleVoteRepository).deleteAll((Iterable<BattleVote>) List.of(battleVote));
                verify(plateRatingRepository).deleteAll((Iterable<PlateRating>) List.of(plateRating));
                verify(commentLikeRepository).deleteAll((Iterable<CommentLike>) List.of(commentLike));
                verify(replyLikeRepository).deleteAll((Iterable<ReplyLike>) List.of(replyLike));
        verify(collectionRepository).deleteAllByUserId(userId);
        verify(collectionProgressRepository).deleteAllByUserId(userId);
        verify(storyInteractionRepository).deleteAllByStoryIdIn(List.of("story-1"));
                verify(storyRepository).saveAll((Iterable<Story>) List.of(story));
        verify(storyInteractionRepository).deleteAllByUserId(userId);
        verify(storyHighlightRepository).deleteAllByUserId(userId);
        verify(conversationRepository).saveAll((Iterable<Conversation>) List.of(conversation));
        verify(chatMessageRepository).saveAll(any());
                verify(eventPublisher, times(1)).publishEvent(org.mockito.ArgumentMatchers.any(PostIndexEvent.class));
    }
}