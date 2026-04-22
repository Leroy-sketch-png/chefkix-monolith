package com.chefkix.culinary.common.scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefkix.culinary.features.session.entity.CookingSession;
import com.chefkix.identity.api.ProfileProvider;
import com.chefkix.identity.api.dto.BasicProfileInfo;
import com.chefkix.shared.event.ReminderEvent;
import com.mongodb.client.result.UpdateResult;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class PostDeadlineSchedulerTest {

    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private ProfileProvider profileProvider;

    @InjectMocks
    private PostDeadlineScheduler scheduler;

    @Test
    void forfeitExpiredSessionsTargetsOnlyCompletedUnpostedSessionsWithPendingXp() {
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(CookingSession.class)))
                .thenReturn(UpdateResult.acknowledged(2L, 2L, null));

        scheduler.forfeitExpiredSessions();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateMulti(queryCaptor.capture(), updateCaptor.capture(), eq(CookingSession.class));

        String queryJson = queryCaptor.getValue().getQueryObject().toString();
        String updateJson = updateCaptor.getValue().getUpdateObject().toString();
        assertThat(queryJson).contains("status=COMPLETED");
        assertThat(queryJson).contains("postId=null");
        assertThat(queryJson).contains("postDeadline=Document{{$lt=");
        assertThat(queryJson).contains("pendingXp=Document{{$gt=0.0}}");
        assertThat(updateJson).contains("status=EXPIRED");
        assertThat(updateJson).contains("pendingXp=0.0");
    }

    @Test
    void checkPostDeadlinesPublishesReminderForMatchingCompletedSession() {
        CookingSession session = CookingSession.builder()
                .id("session-1")
                .userId("user-1")
                .recipeTitle("Spicy Ramen")
                .postDeadline(LocalDateTime.now(ZoneOffset.UTC).plusDays(9))
                .build();

        when(mongoTemplate.find(any(Query.class), eq(CookingSession.class)))
                .thenReturn(List.of(session), List.of(), List.of());
        when(profileProvider.getBasicProfile("user-1")).thenReturn(BasicProfileInfo.builder()
                .userId("user-1")
                .displayName("Chef Kix")
                .build());

        scheduler.checkPostDeadlines();

        ArgumentCaptor<ReminderEvent> reminderCaptor = ArgumentCaptor.forClass(ReminderEvent.class);
        verify(kafkaTemplate).send(eq("reminder-delivery"), reminderCaptor.capture());
        ReminderEvent reminder = reminderCaptor.getValue();

        assertThat(reminder.getUserId()).isEqualTo("user-1");
        assertThat(reminder.getSessionId()).isEqualTo("session-1");
        assertThat(reminder.getReminderType()).isEqualTo("POST_DEADLINE");
        assertThat(reminder.getDaysRemaining()).isEqualTo(9);
        assertThat(reminder.getPriority()).isEqualTo(ReminderEvent.ReminderPriority.NORMAL);
        assertThat(reminder.getContent()).contains("Spicy Ramen", "9 days");
    }
}