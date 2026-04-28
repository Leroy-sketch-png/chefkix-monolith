package com.chefkix.social.provider;

import com.chefkix.social.api.PostProvider;
import com.chefkix.social.api.dto.PostDetail;
import com.chefkix.social.api.dto.PostLinkInfo;
import com.chefkix.social.api.dto.PostSummary;
import com.chefkix.social.api.dto.RecentCookRequest;
import com.chefkix.social.chat.entity.ChatMessage;
import com.chefkix.social.chat.entity.Conversation;
import com.chefkix.social.chat.entity.ParticipantInfo;
import com.chefkix.social.chat.repository.ChatMessageRepository;
import com.chefkix.social.chat.repository.ConversationRepository;
import com.chefkix.social.post.events.PostIndexEvent;
import com.chefkix.social.post.entity.BattleVote;
import com.chefkix.social.post.entity.Comment;
import com.chefkix.social.post.entity.CommentLike;
import com.chefkix.social.post.entity.PlateRating;
import com.chefkix.social.post.entity.Post;
import com.chefkix.social.post.entity.PostLike;
import com.chefkix.social.post.entity.PostSave;
import com.chefkix.social.post.entity.PollVote;
import com.chefkix.social.post.entity.Reply;
import com.chefkix.social.post.entity.ReplyLike;
import com.chefkix.social.post.dto.response.PostResponse;
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
import com.chefkix.social.post.service.PostService;
import com.chefkix.social.group.service.GroupService;
import com.chefkix.social.story.entity.Story;
import com.chefkix.social.story.repository.StoryHighlightRepository;
import com.chefkix.social.story.repository.StoryInteractionRepository;
import com.chefkix.social.story.repository.StoryRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Implements {@link PostProvider} for cross-module consumption.
 * Delegates to PostService and maps internal DTOs to API contract types.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostProviderImpl implements PostProvider {

    private static final String DELETED_USER_DISPLAY_NAME = "Deleted User";
    private static final String DELETED_CONTENT_PLACEHOLDER = "[deleted]";

    PostService postService;
    PostRepository postRepository;
    CommentRepository commentRepository;
    ReplyRepository replyRepository;
    PostLikeRepository postLikeRepository;
    PostSaveRepository postSaveRepository;
    PollVoteRepository pollVoteRepository;
    BattleVoteRepository battleVoteRepository;
    PlateRatingRepository plateRatingRepository;
    CommentLikeRepository commentLikeRepository;
    ReplyLikeRepository replyLikeRepository;
    CollectionRepository collectionRepository;
    CollectionProgressRepository collectionProgressRepository;
    StoryRepository storyRepository;
    StoryInteractionRepository storyInteractionRepository;
    StoryHighlightRepository storyHighlightRepository;
    ConversationRepository conversationRepository;
    ChatMessageRepository chatMessageRepository;
    GroupService groupService;
    ApplicationEventPublisher eventPublisher;

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

    @Override
    public long cleanupDeletedUserData(String userId) {
        long affectedRecords = 0;

        List<Post> posts = postRepository.findAllByUserId(userId);
        if (!posts.isEmpty()) {
            posts.forEach(this::anonymizePost);
            postRepository.saveAll(posts);
            posts.forEach(this::removePostFromIndex);
            affectedRecords += posts.size();
        }

        List<Comment> comments = commentRepository.findAllByUserId(userId);
        if (!comments.isEmpty()) {
            comments.forEach(this::anonymizeComment);
            commentRepository.saveAll(comments);
            affectedRecords += comments.size();
        }

        List<Reply> replies = replyRepository.findAllByUserId(userId);
        if (!replies.isEmpty()) {
            replies.forEach(this::anonymizeReply);
            replyRepository.saveAll(replies);
            affectedRecords += replies.size();
        }

        List<PostLike> postLikes = postLikeRepository.findAllByUserId(userId);
        if (!postLikes.isEmpty()) {
            postLikes.forEach(this::removePostLike);
            postLikeRepository.deleteAll(postLikes);
            affectedRecords += postLikes.size();
        }

        List<PostSave> postSaves = postSaveRepository.findAllByUserId(userId);
        if (!postSaves.isEmpty()) {
            postSaveRepository.deleteAll(postSaves);
            affectedRecords += postSaves.size();
        }

        List<PollVote> pollVotes = pollVoteRepository.findAllByUserId(userId);
        if (!pollVotes.isEmpty()) {
            pollVotes.forEach(this::removePollVote);
            pollVoteRepository.deleteAll(pollVotes);
            affectedRecords += pollVotes.size();
        }

        List<BattleVote> battleVotes = battleVoteRepository.findAllByUserId(userId);
        if (!battleVotes.isEmpty()) {
            battleVotes.forEach(this::removeBattleVote);
            battleVoteRepository.deleteAll(battleVotes);
            affectedRecords += battleVotes.size();
        }

        List<PlateRating> plateRatings = plateRatingRepository.findAllByUserId(userId);
        if (!plateRatings.isEmpty()) {
            plateRatings.forEach(this::removePlateRating);
            plateRatingRepository.deleteAll(plateRatings);
            affectedRecords += plateRatings.size();
        }

        List<CommentLike> commentLikes = commentLikeRepository.findAllByUserId(userId);
        if (!commentLikes.isEmpty()) {
            commentLikes.forEach(this::removeCommentLike);
            commentLikeRepository.deleteAll(commentLikes);
            affectedRecords += commentLikes.size();
        }

        List<ReplyLike> replyLikes = replyLikeRepository.findAllByUserId(userId);
        if (!replyLikes.isEmpty()) {
            replyLikes.forEach(this::removeReplyLike);
            replyLikeRepository.deleteAll(replyLikes);
            affectedRecords += replyLikes.size();
        }

        long collectionsDeleted = collectionRepository.countByUserId(userId);
        if (collectionsDeleted > 0) {
            collectionRepository.deleteAllByUserId(userId);
            affectedRecords += collectionsDeleted;
        }

        List<?> collectionProgress = collectionProgressRepository.findAllByUserId(userId);
        if (!collectionProgress.isEmpty()) {
            collectionProgressRepository.deleteAllByUserId(userId);
            affectedRecords += collectionProgress.size();
        }

        List<Story> stories = storyRepository.findAllByUserId(userId);
        if (!stories.isEmpty()) {
            List<String> storyIds = stories.stream().map(Story::getId).toList();
            storyInteractionRepository.deleteAllByStoryIdIn(storyIds);
            stories.forEach(this::softDeleteStory);
            storyRepository.saveAll(stories);
            affectedRecords += stories.size();
        }

        List<?> storyInteractions = storyInteractionRepository.findAllByUserId(userId);
        if (!storyInteractions.isEmpty()) {
            storyInteractionRepository.deleteAllByUserId(userId);
            affectedRecords += storyInteractions.size();
        }

        List<?> storyHighlights = storyHighlightRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (!storyHighlights.isEmpty()) {
            storyHighlightRepository.deleteAllByUserId(userId);
            affectedRecords += storyHighlights.size();
        }

        List<Conversation> conversations = conversationRepository.findAllByParticipantIdsContains(userId, Sort.unsorted());
        if (!conversations.isEmpty()) {
            conversations.forEach(conversation -> anonymizeConversationParticipant(conversation, userId));
            conversationRepository.saveAll(conversations);
            affectedRecords += conversations.size();
        }

        Map<String, ChatMessage> chatMessagesToSave = new LinkedHashMap<>();

        List<ChatMessage> authoredMessages = chatMessageRepository.findAllBySenderUserId(userId);
        List<String> authoredMessageIds = authoredMessages.stream()
                .map(ChatMessage::getId)
                .filter(Objects::nonNull)
                .toList();
        for (ChatMessage authoredMessage : authoredMessages) {
            tombstoneChatMessage(trackChatMessageMutation(chatMessagesToSave, authoredMessage));
        }

        if (!authoredMessageIds.isEmpty()) {
            List<ChatMessage> replyReferences = chatMessageRepository.findAllByReplyToIdIn(authoredMessageIds);
            for (ChatMessage replyReference : replyReferences) {
                trackChatMessageMutation(chatMessagesToSave, replyReference)
                        .setReplyToSenderName(DELETED_USER_DISPLAY_NAME);
            }
        }

        List<ChatMessage> reactedMessages = chatMessageRepository.findAllByReactionUserId(userId);
        for (ChatMessage reactedMessage : reactedMessages) {
            removeChatReactionsByUser(trackChatMessageMutation(chatMessagesToSave, reactedMessage), userId);
        }

        if (!chatMessagesToSave.isEmpty()) {
            chatMessageRepository.saveAll(chatMessagesToSave.values());
            affectedRecords += chatMessagesToSave.size();
        }

        affectedRecords += groupService.cleanupDeletedUserData(userId);

        return affectedRecords;
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

    private void anonymizePost(Post post) {
        post.setDisplayName(DELETED_USER_DISPLAY_NAME);
        post.setAvatarUrl(null);
        post.setVerified(false);
        post.setContent(DELETED_CONTENT_PLACEHOLDER);
        post.setPhotoUrls(List.of());
        post.setVideoUrl(null);
        post.setSlug(null);
        post.setPostUrl(null);
        post.setRoomCode(null);
        post.setCoChefs(List.of());
        post.setTags(List.of());
        post.setTaggedUserIds(List.of());
        post.setHidden(true);
        post.setStatus(PostStatus.DELETED);
    }

    private void anonymizeComment(Comment comment) {
        comment.setDisplayName(DELETED_USER_DISPLAY_NAME);
        comment.setAvatarUrl(null);
        comment.setContent(DELETED_CONTENT_PLACEHOLDER);
        comment.setTaggedUserIds(List.of());
    }

    private void anonymizeReply(Reply reply) {
        reply.setDisplayName(DELETED_USER_DISPLAY_NAME);
        reply.setAvatarUrl(null);
        reply.setContent(DELETED_CONTENT_PLACEHOLDER);
        reply.setTaggedUserIds(List.of());
    }

    private void removePostLike(PostLike postLike) {
        findPost(postLike.getPostId()).ifPresent(post -> {
            post.setLikes(Math.max(0, defaultZero(post.getLikes()) - 1));
            savePost(post);
        });
    }

    private void removePollVote(PollVote pollVote) {
        findPost(pollVote.getPostId()).ifPresent(post -> {
            if (post.getPollData() == null) {
                return;
            }
            if ("A".equals(pollVote.getOption())) {
                post.getPollData().setVotesA(Math.max(0, defaultZero(post.getPollData().getVotesA()) - 1));
            } else if ("B".equals(pollVote.getOption())) {
                post.getPollData().setVotesB(Math.max(0, defaultZero(post.getPollData().getVotesB()) - 1));
            }
            savePost(post);
        });
    }

    private void removeBattleVote(BattleVote battleVote) {
        findPost(battleVote.getPostId()).ifPresent(post -> {
            if ("A".equals(battleVote.getChoice())) {
                post.setBattleVotesA(Math.max(0, defaultZero(post.getBattleVotesA()) - 1));
            } else if ("B".equals(battleVote.getChoice())) {
                post.setBattleVotesB(Math.max(0, defaultZero(post.getBattleVotesB()) - 1));
            }
            savePost(post);
        });
    }

    private void removePlateRating(PlateRating plateRating) {
        findPost(plateRating.getPostId()).ifPresent(post -> {
            if ("FIRE".equals(plateRating.getRating())) {
                post.setFireCount(Math.max(0, defaultZero(post.getFireCount()) - 1));
            } else if ("CRINGE".equals(plateRating.getRating())) {
                post.setCringeCount(Math.max(0, defaultZero(post.getCringeCount()) - 1));
            }
            savePost(post);
        });
    }

    private void removeCommentLike(CommentLike commentLike) {
        findComment(commentLike.getCommentId()).ifPresent(comment -> {
            comment.setLikes(Math.max(0, defaultZero(comment.getLikes()) - 1));
            commentRepository.save(comment);
        });
    }

    private void removeReplyLike(ReplyLike replyLike) {
        findReply(replyLike.getReplyId()).ifPresent(reply -> {
            reply.setLikes(Math.max(0, defaultZero(reply.getLikes()) - 1));
            replyRepository.save(reply);
        });
    }

    private void removePostFromIndex(Post post) {
        String postId = post.getId();
        if (postId != null) {
            eventPublisher.publishEvent((Object) PostIndexEvent.remove(postId));
        }
    }

    private void anonymizeConversationParticipant(Conversation conversation, String userId) {
        if (conversation.getParticipants() == null || conversation.getParticipants().isEmpty()) {
            return;
        }
        conversation.getParticipants().stream()
                .filter(participant -> Objects.equals(participant.getUserId(), userId))
                .forEach(this::anonymizeParticipantInfo);
    }

    private void tombstoneChatMessage(ChatMessage message) {
        message.setDeleted(true);
        message.setMessage(null);
        message.setRelatedId(null);
        message.setSharedPostImage(null);
        message.setSharedPostTitle(null);
        message.setReplyToContent(null);
        message.setReactions(new ArrayList<>());
        if (message.getSender() != null) {
            anonymizeParticipantInfo(message.getSender());
        }
    }

    private void anonymizeParticipantInfo(ParticipantInfo participantInfo) {
        participantInfo.setUsername(DELETED_USER_DISPLAY_NAME);
        participantInfo.setFirstName(null);
        participantInfo.setLastName(null);
        participantInfo.setAvatar(null);
    }

    private ChatMessage trackChatMessageMutation(Map<String, ChatMessage> chatMessagesToSave, ChatMessage message) {
        if (message == null || message.getId() == null) {
            return message;
        }
        ChatMessage existing = chatMessagesToSave.get(message.getId());
        if (existing != null) {
            return existing;
        }
        chatMessagesToSave.put(message.getId(), message);
        return message;
    }

    private boolean removeChatReactionsByUser(ChatMessage message, String userId) {
        if (message == null || message.getReactions() == null || message.getReactions().isEmpty()) {
            return false;
        }

        boolean changed = false;
        List<ChatMessage.Reaction> filteredReactions = new ArrayList<>();
        for (ChatMessage.Reaction reaction : message.getReactions()) {
            List<String> reactionUserIds = reaction.getUserIds() != null
                    ? new ArrayList<>(reaction.getUserIds())
                    : new ArrayList<>();
            boolean removed = reactionUserIds.removeIf(userId::equals);
            changed = changed || removed;
            if (!reactionUserIds.isEmpty()) {
                reaction.setUserIds(reactionUserIds);
                filteredReactions.add(reaction);
            }
        }

        if (changed) {
            message.setReactions(filteredReactions);
        }
        return changed;
    }

    private Optional<Post> findPost(String postId) {
        if (postId == null) {
            return Optional.empty();
        }
        return postRepository.findById(postId);
    }

    private Optional<Comment> findComment(String commentId) {
        if (commentId == null) {
            return Optional.empty();
        }
        return commentRepository.findById(commentId);
    }

    private Optional<Reply> findReply(String replyId) {
        if (replyId == null) {
            return Optional.empty();
        }
        return replyRepository.findById(replyId);
    }

    private void savePost(Post post) {
        postRepository.save(Objects.requireNonNull(post));
    }

    private int defaultZero(Integer value) {
        return value != null ? value : 0;
    }

    private void softDeleteStory(Story story) {
        story.setIsDeleted(true);
        story.setMediaUrl(null);
        story.setItems(List.of());
        story.setRecipeId(null);
    }
}
