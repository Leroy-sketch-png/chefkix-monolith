package com.chefkix.social.group.specification;

import com.chefkix.social.group.dto.query.GroupExploreQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class GroupSpecification {

    public static Criteria getCriteria(GroupExploreQuery query) {
        List<Criteria> criteriaList = new ArrayList<>();

        // 1. Keyword Search
        if (StringUtils.hasText(query.getKeyword())) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("name").regex(query.getKeyword(), "i"),
                    Criteria.where("description").regex(query.getKeyword(), "i")
            ));
        }

        // 2. Privacy Filter
        if (StringUtils.hasText(query.getPrivacy())) {
            criteriaList.add(Criteria.where("privacyType").is(query.getPrivacy().toUpperCase()));
        }

        // 3. IsJoined Filter
        if (query.getIsJoined() != null && query.getJoinedGroupIds() != null) {
            if (query.getIsJoined()) {
                criteriaList.add(Criteria.where("_id").in(query.getJoinedGroupIds()));
            } else {
                criteriaList.add(Criteria.where("_id").nin(query.getJoinedGroupIds()));
            }
        }

        // 4. Combine
        if (criteriaList.isEmpty()) {
            return new Criteria();
        }
        return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }
}