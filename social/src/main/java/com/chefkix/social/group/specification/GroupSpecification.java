package com.chefkix.social.group.specification;

import com.chefkix.social.group.dto.query.GroupExploreQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class GroupSpecification {

    public static Criteria getCriteria(GroupExploreQuery query) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (StringUtils.hasText(query.getKeyword())) {
            String escaped = Pattern.quote(query.getKeyword().trim());
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("name").regex(escaped, "i"),
                    Criteria.where("description").regex(escaped, "i")
            ));
        }

        if (StringUtils.hasText(query.getPrivacy())) {
            criteriaList.add(Criteria.where("privacyType").is(query.getPrivacy().toUpperCase()));
        }

        if (query.getIsJoined() != null && query.getJoinedGroupIds() != null) {
            if (query.getIsJoined()) {
                criteriaList.add(Criteria.where("_id").in(query.getJoinedGroupIds()));
            } else {
                criteriaList.add(Criteria.where("_id").nin(query.getJoinedGroupIds()));
            }
        }

        if (criteriaList.isEmpty()) {
            return new Criteria();
        }
        return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }
}