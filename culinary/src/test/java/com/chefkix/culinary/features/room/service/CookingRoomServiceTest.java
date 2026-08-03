package com.chefkix.culinary.features.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.common.enums.SessionStatus;
import com.chefkix.culinary.features.room.dto.request.CreateRoomRequest;
import com.chefkix.culinary.features.room.dto.request.JoinRoomRequest;
import com.chefkix.culinary.features.room.dto.response.CookingRoomResponse;
import com.chefkix.culinary.features.room.model.CookingRoom;
import com.chefkix.culinary.features.room.model.RoomEvent;
import com.chefkix.culinary.features.room.model.RoomEventType;
import com.chefkix.culinary.features.room.model.RoomParticipant;
import com.chefkix.culinary.features.room.repository.CookingRoomRedisRepository;
import com.chefkix.culinary.features.session.dto.request.StartSessionRequest;
import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.culinary.features.session.repository.CookingSessionRepository;
import com.chefkix.culinary.features.session.service.CookingSessionService;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class CookingRoomServiceTest {

    @Mock
    private CookingRoomRedisRepository roomRepository;
    @Mock
    private CookingSessionService sessionService;
    @Mock
    private CookingSessionRepository sessionRepository;
    @Mock
    private ProfileProvider profileProvider;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CookingRoomService roomService;

    @ParameterizedTest
    @EnumSource(value = SessionStatus.class, names = {"IN_PROGRESS", "PAUSED"})
    void createRoomReusesEverySameRecipeResumableSession(SessionStatus status) {
        CookingSession existing = CookingSession.builder()
                .id("session-1")
                .userId("user-1")
                .recipeId("recipe-1")
                .recipeTitle("Spicy Noodles")
                .status(status)
                .roomCode("EXPIRED")
                .build();
        when(roomRepository.exists(anyString())).thenReturn(false);
        when(sessionRepository.findFirstByUserIdAndStatusIn(
                "user-1",
                List.of(SessionStatus.IN_PROGRESS, SessionStatus.PAUSED)
        )).thenReturn(Optional.of(existing));
        when(profileProvider.getBasicProfile("user-1")).thenReturn(profile());
        when(sessionRepository.findById("session-1")).thenReturn(Optional.of(existing));

        CookingRoomResponse response = roomService.createRoom(
                "user-1",
                CreateRoomRequest.builder().recipeId("recipe-1").build()
        );

        assertThat(response.getSessionId()).isEqualTo("session-1");
        assertThat(existing.getStatus()).isEqualTo(status);
        assertThat(existing.getRoomCode()).isEqualTo(response.getRoomCode());
        verify(sessionRepository).save(existing);
        verify(sessionService, never()).startSession(anyString(), any(StartSessionRequest.class));

        ArgumentCaptor<CookingRoom> roomCaptor = ArgumentCaptor.forClass(CookingRoom.class);
        verify(roomRepository).save(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getParticipants()).singleElement()
                .satisfies(participant -> assertThat(participant.getSessionId()).isEqualTo("session-1"));
    }

    @Test
    void createRoomPreservesDifferentRecipeSessionAsABlocker() {
        CookingSession existing = CookingSession.builder()
                .id("session-1")
                .userId("user-1")
                .recipeId("other-recipe")
                .status(SessionStatus.PAUSED)
                .build();
        when(roomRepository.exists(anyString())).thenReturn(false);
        when(sessionRepository.findFirstByUserIdAndStatusIn(
                "user-1",
                List.of(SessionStatus.IN_PROGRESS, SessionStatus.PAUSED)
        )).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> roomService.createRoom(
                "user-1",
                CreateRoomRequest.builder().recipeId("recipe-1").build()
        )).isInstanceOfSatisfying(AppException.class, error ->
                assertThat(error.getErrorCode()).isEqualTo(ErrorCode.SESSION_ALREADY_ACTIVE)
        );

        assertThat(existing.getRoomCode()).isNull();
        verify(sessionRepository, never()).save(any(CookingSession.class));
        verify(roomRepository, never()).save(any(CookingRoom.class));
        verify(profileProvider, never()).getBasicProfile(anyString());
    }

    @Test
    void createRoomRejectsASecondActiveRoomBeforeChangingSessionState() {
        when(roomRepository.findAll()).thenReturn(List.of(activeRoom("ROOM01", "user-1")));

        assertThatThrownBy(() -> roomService.createRoom(
                "user-1",
                CreateRoomRequest.builder().recipeId("recipe-1").build()
        )).isInstanceOfSatisfying(AppException.class, error ->
                assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ALREADY_IN_ROOM)
        );

        verify(roomRepository, never()).exists(anyString());
        verify(sessionRepository, never()).findFirstByUserIdAndStatusIn(anyString(), any());
        verify(sessionService, never()).startSession(anyString(), any(StartSessionRequest.class));
    }

    @Test
    void joinRoomRejectsMembershipInAnotherActiveRoom() {
        CookingRoom target = activeRoom("TARGET", "host-1");
        CookingRoom existing = activeRoom("OTHER1", "user-1");
        when(roomRepository.findByRoomCode("TARGET")).thenReturn(Optional.of(target));
        when(roomRepository.findAll()).thenReturn(List.of(target, existing));

        assertThatThrownBy(() -> roomService.joinRoom(
                "user-1",
                JoinRoomRequest.builder().roomCode("TARGET").build()
        )).isInstanceOfSatisfying(AppException.class, error ->
                assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ALREADY_IN_ROOM)
        );

        verify(sessionRepository, never()).findFirstByUserIdAndStatusIn(anyString(), any());
        verify(roomRepository, never()).save(any(CookingRoom.class));
    }

    @Test
    void joinRoomUpgradesSameRoomSpectatorWithoutRemovingOrDuplicatingMembership() {
        Instant joinedAt = Instant.parse("2026-07-28T08:00:00Z");
        RoomParticipant host = RoomParticipant.builder()
                .userId("host-1")
                .role("COOK")
                .build();
        RoomParticipant spectator = RoomParticipant.builder()
                .userId("user-1")
                .displayName("Test User")
                .role("SPECTATOR")
                .joinedAt(joinedAt)
                .build();
        CookingRoom target = CookingRoom.builder()
                .roomCode("TARGET")
                .recipeId("recipe-1")
                .status(CookingRoom.STATUS_WAITING)
                .maxParticipants(2)
                .participants(List.of(host, spectator))
                .build();
        CookingSession existing = CookingSession.builder()
                .id("session-1")
                .userId("user-1")
                .recipeId("recipe-1")
                .status(SessionStatus.PAUSED)
                .build();
        when(roomRepository.findByRoomCode("TARGET")).thenReturn(Optional.of(target));
        when(sessionRepository.findFirstByUserIdAndStatusIn(
                "user-1",
                List.of(SessionStatus.IN_PROGRESS, SessionStatus.PAUSED)
        )).thenReturn(Optional.of(existing));

        CookingRoomResponse response = roomService.joinRoom(
                "user-1",
                JoinRoomRequest.builder().roomCode("TARGET").role("COOK").build()
        );

        assertThat(response.getSessionId()).isEqualTo("session-1");
        assertThat(response.getParticipants()).hasSize(2);
        assertThat(spectator.getRole()).isEqualTo("COOK");
        assertThat(spectator.getSessionId()).isEqualTo("session-1");
        assertThat(spectator.getJoinedAt()).isEqualTo(joinedAt);
        assertThat(target.getStatus()).isEqualTo(CookingRoom.STATUS_COOKING);
        assertThat(existing.getRoomCode()).isEqualTo("TARGET");
        verify(roomRepository).save(target);
        verify(sessionRepository).save(existing);
        verify(profileProvider, never()).getBasicProfile(anyString());

        ArgumentCaptor<RoomEvent> eventCaptor = ArgumentCaptor.forClass(RoomEvent.class);
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/room/TARGET"),
                eventCaptor.capture()
        );
        assertThat(eventCaptor.getValue().getType()).isEqualTo(RoomEventType.PARTICIPANT_ROLE_CHANGED);
        assertThat(eventCaptor.getValue().getData()).containsEntry("role", "COOK");
    }

    @Test
    void joinRoomKeepsSpectatorUntouchedWhenCookSessionIsBlocked() {
        RoomParticipant spectator = RoomParticipant.builder()
                .userId("user-1")
                .role("SPECTATOR")
                .build();
        CookingRoom target = CookingRoom.builder()
                .roomCode("TARGET")
                .recipeId("recipe-1")
                .status(CookingRoom.STATUS_WAITING)
                .participants(List.of(spectator))
                .build();
        CookingSession blocker = CookingSession.builder()
                .id("session-1")
                .userId("user-1")
                .recipeId("other-recipe")
                .status(SessionStatus.IN_PROGRESS)
                .build();
        when(roomRepository.findByRoomCode("TARGET")).thenReturn(Optional.of(target));
        when(sessionRepository.findFirstByUserIdAndStatusIn(
                "user-1",
                List.of(SessionStatus.IN_PROGRESS, SessionStatus.PAUSED)
        )).thenReturn(Optional.of(blocker));

        assertThatThrownBy(() -> roomService.joinRoom(
                "user-1",
                JoinRoomRequest.builder().roomCode("TARGET").role("COOK").build()
        )).isInstanceOfSatisfying(AppException.class, error ->
                assertThat(error.getErrorCode()).isEqualTo(ErrorCode.SESSION_ALREADY_ACTIVE)
        );

        assertThat(spectator.getRole()).isEqualTo("SPECTATOR");
        assertThat(spectator.getSessionId()).isNull();
        verify(roomRepository, never()).save(any(CookingRoom.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    private static BasicProfileInfo profile() {
        return BasicProfileInfo.builder()
                .userId("user-1")
                .username("testuser")
                .displayName("Test User")
                .build();
    }

    private static CookingRoom activeRoom(String roomCode, String userId) {
        return CookingRoom.builder()
                .roomCode(roomCode)
                .recipeId("recipe-1")
                .status(CookingRoom.STATUS_WAITING)
                .participants(List.of(RoomParticipant.builder()
                        .userId(userId)
                        .role("COOK")
                        .build()))
                .build();
    }
}
