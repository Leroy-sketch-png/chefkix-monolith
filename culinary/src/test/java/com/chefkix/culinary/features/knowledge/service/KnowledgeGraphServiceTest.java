package com.chefkix.culinary.features.knowledge.service;

import com.chefkix.culinary.features.knowledge.entity.KnowledgeIngredient;
import com.chefkix.culinary.features.knowledge.repository.KnowledgeIngredientRepository;
import com.chefkix.culinary.features.knowledge.repository.KnowledgeTechniqueRepository;
import com.chefkix.shared.exception.AppException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphServiceTest {
    @Mock KnowledgeIngredientRepository ingredientRepo;
    @Mock KnowledgeTechniqueRepository techniqueRepo;
    @InjectMocks KnowledgeGraphService service;

    @Test
    void initialPageIsBoundedAndRatioIsNotConfidence() {
        var butter = ingredient("butter", "coconut oil", 0.75);
        var coconut = ingredient("coconut oil", null, null);
        var request = PageRequest.of(0, 2, Sort.by("canonicalName"));
        when(ingredientRepo.count()).thenReturn(3L);
        when(ingredientRepo.findAll(request)).thenReturn(new PageImpl<>(List.of(butter, coconut), request, 3));

        var graph = service.getGraph(null, null, 1, 2);

        assertThat(graph.nodes()).hasSize(2);
        assertThat(graph.totalNodeCount()).isEqualTo(3);
        assertThat(graph.hasMore()).isTrue();
        assertThat(graph.edges()).hasSize(1);
        assertThat(graph.edges().getFirst().confidence()).isNull();
        assertThat(graph.edges().getFirst().substitutionRatio()).isEqualTo(0.75);
        verify(ingredientRepo, never()).findAll();
        verify(ingredientRepo, never()).findByCanonicalName("coconut oil");
    }

    @Test
    void rootNeighborhoodExpandsInBatchesToRequestedDepth() {
        var butter = ingredient("butter", "coconut oil", 0.75);
        var coconut = ingredient("coconut oil", "olive oil", 1.0);
        var olive = ingredient("olive oil", null, null);
        when(ingredientRepo.count()).thenReturn(3L);
        when(ingredientRepo.findByCanonicalName("butter")).thenReturn(Optional.of(butter));
        when(ingredientRepo.findByCanonicalNameIn(List.of("coconut oil"))).thenReturn(List.of(coconut));
        when(ingredientRepo.findByCanonicalNameIn(List.of("olive oil"))).thenReturn(List.of(olive));

        var graph = service.getGraph(" BUTTER ", null, 2, 3);

        assertThat(graph.nodes()).extracting(node -> node.id())
                .containsExactly("butter", "coconut oil", "olive oil");
        assertThat(graph.edges()).hasSize(2);
        assertThat(graph.hasMore()).isFalse();
        verify(ingredientRepo, times(2)).findByCanonicalNameIn(anyCollection());
        verify(ingredientRepo, never()).findAll();
    }

    @Test
    void rootLimitStopsExpansionBeforeFetchingNeighbors() {
        var butter = ingredient("butter", "coconut oil", 0.75);
        when(ingredientRepo.count()).thenReturn(3L);
        when(ingredientRepo.findByCanonicalName("butter")).thenReturn(Optional.of(butter));

        var graph = service.getGraph("butter", null, 2, 1);

        assertThat(graph.nodes()).hasSize(1);
        assertThat(graph.edges()).isEmpty();
        assertThat(graph.hasMore()).isTrue();
        verify(ingredientRepo, never()).findByCanonicalNameIn(anyCollection());
    }

    @Test
    void searchEscapesRegexAndReturnsOnlyPage() {
        var request = PageRequest.of(0, 20, Sort.by("canonicalName"));
        when(ingredientRepo.count()).thenReturn(30L);
        when(ingredientRepo.searchByNameOrAlias(Pattern.quote("pea.*"), request))
                .thenReturn(new PageImpl<>(List.of(ingredient("peanut", null, null)), request, 1));

        var graph = service.getGraph(null, "pea.*", 0, 20);

        assertThat(graph.nodes()).extracting(node -> node.id()).containsExactly("peanut");
        assertThat(graph.hasMore()).isFalse();
        verify(ingredientRepo, never()).findAll();
    }

    @Test
    void conflictingQueryAndMissingRootFailClearly() {
        assertThatThrownBy(() -> service.getGraph("butter", "pea", 1, 10))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Use either root or q");
        assertThatThrownBy(() -> service.getGraph("missing", null, 1, 10))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Knowledge ingredient not found");
    }

    private static KnowledgeIngredient ingredient(String name, String alternative, Double ratio) {
        return KnowledgeIngredient.builder()
                .canonicalName(name)
                .name(name)
                .category("ingredient")
                .allergenFlags(List.of())
                .substitutions(alternative == null ? List.of() : List.of(
                        KnowledgeIngredient.Substitution.builder()
                                .alternative(alternative).context("recorded context").ratio(ratio).build()))
                .build();
    }
}
