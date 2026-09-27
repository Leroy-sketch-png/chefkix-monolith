package com.chefkix.culinary.features.knowledge.controller;

import com.chefkix.culinary.features.knowledge.dto.KnowledgeGraphResponse;
import com.chefkix.culinary.features.knowledge.service.KnowledgeGraphService;
import com.chefkix.shared.exception.AppException;
import com.chefkix.shared.exception.ErrorCode;
import com.chefkix.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KnowledgeGraphControllerTest {
    @Test
    void bindsBoundedGraphQueryAndSerializesCounts() throws Exception {
        var service = mock(KnowledgeGraphService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new KnowledgeGraphController(service)).build();
        when(service.getGraph("butter", null, 2, 25))
                .thenReturn(new KnowledgeGraphResponse(List.of(), List.of(), 24, true));

        mvc.perform(get("/knowledge/graph")
                        .param("root", "butter").param("depth", "2").param("limit", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalNodeCount").value(24))
                .andExpect(jsonPath("$.data.hasMore").value(true));

        verify(service).getGraph("butter", null, 2, 25);
    }

    @Test
    void missingRootReturnsNotFoundThroughSharedHandler() throws Exception {
        var service = mock(KnowledgeGraphService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new KnowledgeGraphController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        when(service.getGraph("missing", null, 1, 100))
                .thenThrow(new AppException(ErrorCode.KNOWLEDGE_INGREDIENT_NOT_FOUND));

        mvc.perform(get("/knowledge/graph").param("root", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
