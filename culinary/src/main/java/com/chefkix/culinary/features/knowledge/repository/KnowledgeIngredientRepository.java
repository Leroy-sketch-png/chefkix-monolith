package com.chefkix.culinary.features.knowledge.repository;

import com.chefkix.culinary.features.knowledge.entity.KnowledgeIngredient;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface KnowledgeIngredientRepository extends MongoRepository<KnowledgeIngredient, String> {

    Optional<KnowledgeIngredient> findByCanonicalName(String canonicalName);

    List<KnowledgeIngredient> findByCanonicalNameIn(Collection<String> canonicalNames);

    List<KnowledgeIngredient> findByCategory(String category);

    @Query("{ '$or': [ " +
            "{ 'canonicalName': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'name': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'aliases': { '$regex': ?0, '$options': 'i' } } " +
            "] }")
    List<KnowledgeIngredient> searchByNameOrAlias(String pattern);

    @Query("{ '$or': [ " +
            "{ 'canonicalName': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'name': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'aliases': { '$regex': ?0, '$options': 'i' } } " +
            "] }")
    Page<KnowledgeIngredient> searchByNameOrAlias(String pattern, Pageable pageable);

    @Query("{ 'substitutions': { '$exists': true, '$ne': [] } }")
    List<KnowledgeIngredient> findAllWithSubstitutions();

    boolean existsByCanonicalName(String canonicalName);
}
