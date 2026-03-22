package com.chefkix.culinary.features.knowledge.repository;

import com.chefkix.culinary.features.knowledge.entity.KnowledgeTechnique;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface KnowledgeTechniqueRepository extends MongoRepository<KnowledgeTechnique, String> {

    Optional<KnowledgeTechnique> findByCanonicalName(String canonicalName);

    List<KnowledgeTechnique> findByCategory(String category);

    List<KnowledgeTechnique> findByDifficulty(String difficulty);

    @Query("{ '$or': [ " +
            "{ 'canonicalName': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'name': { '$regex': ?0, '$options': 'i' } } " +
            "] }")
    List<KnowledgeTechnique> searchByName(String pattern);

    boolean existsByCanonicalName(String canonicalName);
}
