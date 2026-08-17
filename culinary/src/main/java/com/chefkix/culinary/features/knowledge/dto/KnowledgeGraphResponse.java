package com.chefkix.culinary.features.knowledge.dto;

import java.util.List;

public record KnowledgeGraphResponse(List<Node> nodes, List<Edge> edges) {
    public record Node(String id, String name, String category, List<String> allergenFlags) {}
    public record Edge(String source, String target, String type, Double confidence, String context) {}
}
