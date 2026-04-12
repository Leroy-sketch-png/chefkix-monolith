package com.chefkix.social.group.repository.custom;

import com.chefkix.social.group.dto.query.GroupExploreQuery;
import com.chefkix.social.group.entity.Group;
import com.chefkix.social.group.specification.GroupSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class GroupRepositoryImpl implements GroupCustomRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Group> searchGroups(GroupExploreQuery queryDto, Pageable pageable) {

        // 1. Use Specification to build filters
        Criteria criteria = GroupSpecification.getCriteria(queryDto);

        // 2. Create Query
        Query query = new Query(criteria).with(pageable);

        // 3. Apply Custom Sort
        applyCustomSorting(query, queryDto.getSortBy());

        // 4. Execute Query to fetch data
        List<Group> groups = mongoTemplate.find(query, Group.class);

        // 5. Calculate total
        return PageableExecutionUtils.getPage(
                groups,
                pageable,
                () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Group.class)
        );
    }

    private void applyCustomSorting(Query query, String sortBy) {
        if ("popular".equalsIgnoreCase(sortBy)) {
            // Sort by largest groups first
            query.with(Sort.by(Sort.Direction.DESC, "memberCount"));
        }
        // Default Spring Pageable sorting handles the rest
    }
}