package com.chefkix.social.group.publisher;

import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.shared.event.*;
import com.chefkix.social.group.entity.Group;
import com.chefkix.social.group.enums.MemberStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupEventPublisher {

    KafkaTemplate<String, Object> kafkaTemplate;
    ProfileProvider profileProvider;

    @Qualifier("taskExecutor")
    Executor taskExecutor;

    public void publishMembershipEvent(Group group, String targetUserId, MemberStatus status) {
        CompletableFuture.runAsync(() -> {
            try {
                BasicProfileInfo profile = null;
                try {
                    profile = profileProvider.getBasicProfile(targetUserId);
                } catch (Exception ignored) {
                    log.warn("Could not fetch profile for user {}", targetUserId);
                }

                String displayName = profile != null ? profile.getDisplayName() : "A new user";
                String avatarUrl = profile != null ? profile.getAvatarUrl() : null;

                BaseEvent event;
                if (status == MemberStatus.ACTIVE) {
                    event = GroupMemberJoinedEvent.builder()
                            .groupId(group.getId())
                            .groupName(group.getName())
                            .memberId(targetUserId)
                            .memberDisplayName(displayName)
                            .memberAvatarUrl(avatarUrl)
                            .adminId(group.getOwnerId())
                            .build();
                } else {
                    event = GroupJoinRequestedEvent.builder()
                            .groupId(group.getId())
                            .groupName(group.getName())
                            .requesterId(targetUserId)
                            .requesterDisplayName(displayName)
                            .requesterAvatarUrl(avatarUrl)
                            .adminId(group.getOwnerId())
                            .build();
                }

                kafkaTemplate.send("group-delivery", event);
                log.info("Published {} for group {}", event.getEventType(), group.getId());

            } catch (Exception e) {
                log.error("Failed to publish group membership event", e);
            }
        }, taskExecutor);
    }

    public void publishRequestApprovedEvent(Group group, String targetUserId, String adminId) {
        CompletableFuture.runAsync(() -> {
            try {
                GroupRequestApprovedEvent event = GroupRequestApprovedEvent.builder()
                        .groupId(group.getId())
                        .groupName(group.getName())
                        .groupCoverImageUrl(group.getCoverImageUrl())
                        .requesterId(targetUserId)
                        .adminId(adminId)
                        .build();

                kafkaTemplate.send("group-delivery", event);
                log.info("Published GROUP_REQUEST_APPROVED for group {}", group.getId());

            } catch (Exception e) {
                log.error("Failed to publish approval event", e);
            }
        }, taskExecutor);
    }

    public void publishOwnershipTransferredEvent(Group group, String newOwnerId, String oldOwnerId) {
        CompletableFuture.runAsync(() -> {
            try {
                GroupOwnershipTransferredEvent event = GroupOwnershipTransferredEvent.builder()
                        .groupId(group.getId())
                        .groupName(group.getName())
                        .groupCoverImageUrl(group.getCoverImageUrl())
                        .newOwnerId(newOwnerId)
                        .oldOwnerId(oldOwnerId)
                        .build();

                kafkaTemplate.send("group-delivery", event);
                log.info("Published GROUP_OWNERSHIP_TRANSFERRED for group {}", group.getId());

            } catch (Exception e) {
                log.error("Failed to publish ownership transfer event", e);
            }
        }, taskExecutor);
    }
}