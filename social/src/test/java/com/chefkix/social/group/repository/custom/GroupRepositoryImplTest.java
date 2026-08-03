package com.chefkix.social.group.repository.custom;

import com.chefkix.social.group.dto.query.GroupExploreQuery;
import com.chefkix.social.group.entity.Group;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupRepositoryImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private GroupRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new GroupRepositoryImpl(mongoTemplate);
        when(mongoTemplate.find(any(Query.class), eq(Group.class))).thenReturn(List.of());
    }

    @Test
    void newestSortIsDeterministic() {
        repository.searchGroups(
                GroupExploreQuery.builder().sortBy("newest").build(),
                PageRequest.of(0, 12)
        );

        Query query = captureQuery();
        assertThat(query.getSortObject()).containsEntry("createdAt", -1);
    }

    @Test
    void popularSortUsesMemberCountThenRecency() {
        repository.searchGroups(
                GroupExploreQuery.builder().sortBy("popular").build(),
                PageRequest.of(0, 12)
        );

        Query query = captureQuery();
        assertThat(query.getSortObject())
                .containsEntry("memberCount", -1)
                .containsEntry("createdAt", -1);
    }

    private Query captureQuery() {
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(Group.class));
        return queryCaptor.getValue();
    }
}
