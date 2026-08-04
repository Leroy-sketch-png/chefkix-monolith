package com.chefkix.culinary.features.recipe.repository.custom;

import com.chefkix.culinary.common.dto.query.RecipeSearchQuery;
import com.chefkix.culinary.common.enums.RecipeStatus;
import com.chefkix.culinary.common.enums.RecipeVisibility;
import com.chefkix.culinary.features.recipe.entity.Recipe;
import com.chefkix.culinary.common.specification.RecipeSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Repository
@RequiredArgsConstructor
public class RecipeRepositoryImpl implements RecipeRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Recipe> searchRecipes(RecipeSearchQuery queryDto, Pageable pageable) {
        Criteria criteria = RecipeSpecification.getCriteria(queryDto);

        Query query = new Query(criteria).with(pageable);

        applyCustomSorting(query, queryDto.getSortBy());

        long total = mongoTemplate.count(Query.of(query).limit(0).skip(0), Recipe.class);

        List<Recipe> recipes = mongoTemplate.find(query, Recipe.class);

        return new PageImpl<>(recipes, pageable, total);
    }

    @Override
    public void incrementViewCount(String recipeId) {
        Query query = Query.query(Criteria.where("id").is(recipeId));
        Update update = new Update().inc("viewCount", 1);
        mongoTemplate.updateFirst(query, update, Recipe.class);
    }

    private void applyCustomSorting(Query query, String sortBy) {
        if ("trending".equalsIgnoreCase(sortBy)) {
            query.with(Sort.by(Sort.Direction.DESC, "viewCount", "likeCount"));
        }
        else if ("xpReward".equalsIgnoreCase(sortBy)) {
            query.with(Sort.by(Sort.Direction.DESC, "xpReward"));
        }
    }

    @Override
    public Recipe updateLikeCount(String recipeId, int amount) {
        return updateCounter(recipeId, "likeCount", amount);
    }

    @Override
    public Recipe updateSaveCount(String recipeId, int amount) {
        return updateCounter(recipeId, "saveCount", amount);
    }

    private Recipe updateCounter(String recipeId, String fieldName, int amount) {
        Query query = Query.query(Criteria.where("id").is(recipeId));

        if (amount < 0) {
            query.addCriteria(Criteria.where(fieldName).gt(0));
        }

        Update update = new Update().inc(fieldName, amount);

        return mongoTemplate.findAndModify(
                query, update,
                FindAndModifyOptions.options().returnNew(true),
                Recipe.class
        );
    }

    @Override
    public List<Recipe> findPublishedForIngredientMatching() {
        Query query = Query.query(Criteria.where("status").is(RecipeStatus.PUBLISHED));
        query.fields()
                .include("id")
                .include("title")
                .include("coverImageUrl")
                .include("prepTimeMinutes")
                .include("cookTimeMinutes")
                .include("totalTimeMinutes")
                .include("servings")
                .include("difficulty")
                .include("cuisineType")
                .include("dietaryTags")
                .include("mealRole")
                .include("fullIngredientList");
        return mongoTemplate.find(query, Recipe.class);
    }

    @Override
    public List<Recipe> findChallengeCandidates(Map<String, Object> challengeCriteria, int limit) {
        List<Criteria> filters = new java.util.ArrayList<>();
        filters.add(Criteria.where("status").is(RecipeStatus.PUBLISHED));
        filters.add(new Criteria().orOperator(
                Criteria.where("recipeVisibility").is(RecipeVisibility.PUBLIC),
                Criteria.where("recipeVisibility").is(null)));

        Integer maxTimeMinutes = numericCriteria(challengeCriteria, "maxTimeMinutes");
        if (maxTimeMinutes != null) {
            filters.add(Criteria.where("totalTimeMinutes").gt(0).lte(maxTimeMinutes));
        }

        addPatternFilter(filters, challengeCriteria, "cuisineType", "cuisineType", true);
        addPatternFilter(filters, challengeCriteria, "ingredientContains", "fullIngredientList.name", false);
        addPatternFilter(filters, challengeCriteria, "dietaryTags", "dietaryTags", true);
        addPatternFilter(filters, challengeCriteria, "skillTags", "skillTags", true);

        addPatternFilter(filters, challengeCriteria, "difficulty", "difficulty", true);

        Query query = new Query(new Criteria().andOperator(filters.toArray(Criteria[]::new)))
                .with(Sort.by(Sort.Direction.DESC, "cookCount", "viewCount"))
                .limit(Math.max(1, Math.min(limit, 20)));
        return mongoTemplate.find(query, Recipe.class);
    }

    private void addPatternFilter(
            List<Criteria> filters,
            Map<String, Object> challengeCriteria,
            String criteriaKey,
            String documentField,
            boolean exactMatch) {
        List<String> values = stringListCriteria(challengeCriteria, criteriaKey);
        if (values.isEmpty()) return;

        List<Pattern> patterns = values.stream()
                .map(value -> Pattern.compile(
                        exactMatch ? "^" + Pattern.quote(value) + "$" : Pattern.quote(value),
                        Pattern.CASE_INSENSITIVE))
                .toList();
        filters.add(Criteria.where(documentField).in(patterns));
    }

    private List<String> stringListCriteria(Map<String, Object> criteria, String key) {
        if (criteria == null || !(criteria.get(key) instanceof List<?> values)) {
            return List.of();
        }
        return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(value -> !value.isBlank())
                .filter(value -> !"ANY".equalsIgnoreCase(value))
                .toList();
    }

    private Integer numericCriteria(Map<String, Object> criteria, String key) {
        if (criteria != null && criteria.get(key) instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        return null;
    }
}
