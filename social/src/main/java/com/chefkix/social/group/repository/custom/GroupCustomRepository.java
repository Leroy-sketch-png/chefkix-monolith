package com.chefkix.social.group.repository.custom;

import com.chefkix.social.group.dto.query.GroupExploreQuery;
import com.chefkix.social.group.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GroupCustomRepository {
    Page<Group> searchGroups(GroupExploreQuery query, Pageable pageable);
}