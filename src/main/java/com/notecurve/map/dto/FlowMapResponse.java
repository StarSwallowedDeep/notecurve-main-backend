package com.notecurve.map.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowMapResponse {
    private List<Node> nodes;
    private List<Edge> edges;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Node {
        private String id;
        private String type;
        private NodeData data;
        private List<String> groupNodes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NodeData {
        private String label;
        private String type;
        private String method;
        private String color;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Edge {
        private String id;
        private String source;
        private String target;
        private String sourceMethod;
        private String targetMethod;
        private String label;
        private String type;
        private java.util.Map<String, List<String>> dtoDetails;
    }
}
