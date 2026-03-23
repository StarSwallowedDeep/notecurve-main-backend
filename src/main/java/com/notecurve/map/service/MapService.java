package com.notecurve.map.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.notecurve.map.dto.FlowMapResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class MapService {

    public FlowMapResponse analyzeMultipartFiles(List<MultipartFile> files) {
        List<FlowMapResponse.Node> nodes = new ArrayList<>();
        List<FlowMapResponse.Edge> edges = new ArrayList<>();

        // DTO 필드 정보 및 클래스 간의 단순 의존성 맵 생성
        java.util.Map<String, List<String>> dtoFieldMap = new java.util.HashMap<>();
        java.util.Map<String, List<String>> classDependencyMap = new java.util.HashMap<>();

        // 각 파일을 순회하며 노드(Node)와 에지(Edge) 생성
        Set<String> analyzedClassNames = new HashSet<>();
        for (MultipartFile file : files) {
            if (file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".java")) continue;
            try (var is = file.getInputStream()) {
                CompilationUnit cu = StaticJavaParser.parse(is);
                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDef -> {
                    String t = determineType(classDef);
                    if (t.equals("DTO")) {
                        List<String> fields = classDef.getFields().stream()
                            .map(f -> f.getElementType().asString() + " " + f.getVariable(0).getNameAsString())
                            .toList();
                        dtoFieldMap.put(classDef.getNameAsString(), fields);
                    } else {
                        analyzedClassNames.add(classDef.getNameAsString());
                    }
                });
            } catch (Exception ignored) {}
        }

        for (MultipartFile file : files) {
            if (file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".java")) continue;
            try (var is = file.getInputStream()) {
                CompilationUnit cu = StaticJavaParser.parse(is);
                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDef -> {

                    // 클래스 타입(Controller, Service 등) 판별 및 노드 데이터 구성
                    String className = classDef.getNameAsString();
                    List<String> deps = new ArrayList<>();
                    classDef.getFields().forEach(f -> {
                        if (f.isStatic() && f.isFinal()) return;
                        String target = f.getElementType().asString();
                        if (target.endsWith("Service") || target.endsWith("Repository")) {
                            deps.add(target);
                        }
                    });
                    if (!deps.isEmpty()) classDependencyMap.put(className, deps);
                });
            } catch (Exception ignored) {}
        }

        for (MultipartFile file : files) {
            if (file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".java")) continue;

            try (var is = file.getInputStream()) {
                CompilationUnit cu = StaticJavaParser.parse(is);
                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDef -> {
                    String className = classDef.getNameAsString();
                    String type = determineType(classDef);
                    
                    if (type.equals("DTO")) return; // DTO는 노드로 직접 표시하지 않고 관계 정보로만 사용

                    String subInfo = extractDetailedSubInfo(classDef, type);
                    String color = getColorByType(type);

                    nodes.add(FlowMapResponse.Node.builder()
                            .id(className)
                            .type("codeCard")
                            .data(FlowMapResponse.NodeData.builder()
                                    .label(className)
                                    .type(type)
                                    .method(subInfo)
                                    .color(getColorByType(type))
                                    .build())
                            .build());

                    // Controller를 기준으로 관련 Service/Repository를 하나의 그룹(Domain)으로 묶음
                    if (type.equals("CONTROLLER")) {
                        String baseMapping = classDef.getAnnotationByName("RequestMapping")
                            .map(a -> {
                                String s = a.toString();
                                return s.contains("\"") ? s.substring(s.indexOf("\"") + 1, s.lastIndexOf("\"")) : "";
                            }).orElse("");

                        if (!baseMapping.isEmpty()) {
                            String domainNodeId = "DOMAIN-" + className;

                            List<String> groupMembers = new ArrayList<>();
                            groupMembers.add(className);

                            List<String> directDeps = classDependencyMap.getOrDefault(className, new ArrayList<>());
                            for (String dep : directDeps) {
                                if (!analyzedClassNames.contains(dep)) continue;
                                groupMembers.add(dep);
                                List<String> subDeps = classDependencyMap.getOrDefault(dep, new ArrayList<>());
                                for (String subDep : subDeps) {
                                    if (subDep.endsWith("Repository") 
                                        && analyzedClassNames.contains(subDep)
                                        && !groupMembers.contains(subDep)) {
                                        groupMembers.add(subDep);
                                    }
                                }
                            }

                            nodes.add(FlowMapResponse.Node.builder()
                                .id(domainNodeId)
                                .type("codeCard")
                                .data(FlowMapResponse.NodeData.builder()
                                    .label("📄" + baseMapping + " 관리")
                                    .type("DOMAIN")
                                    .method("API Endpoint")
                                    .color("#8b5cf6")
                                    .build())
                                .groupNodes(groupMembers)
                                .build());

                            edges.add(FlowMapResponse.Edge.builder()
                                .id("DOMAIN-EDGE-" + className)
                                .source(domainNodeId)
                                .target(className)
                                .type("DOMAIN")
                                .label("")
                                .build());
                        }
                    }

                    // 메서드 호출(Call Graph) 및 필드 주입 관계 분석
                    extractAdvancedRelationships(classDef, className, edges, dtoFieldMap);
                });
            } catch (Exception e) {
                System.err.println("분석 실패: " + file.getOriginalFilename());
            }
        }
        return new FlowMapResponse(nodes, edges);
    }

    private String extractDetailedSubInfo(ClassOrInterfaceDeclaration classDef, String type) {
        if (type.equals("ENTITY")) {
            List<String> fields = classDef.getFields().stream()
                    .map(f -> f.getVariable(0).getNameAsString())
                    .toList();
            
            StringBuilder table = new StringBuilder();
            table.append("🗄️ DATABASE TABLE\n");
            table.append("----------------------------\n");
            table.append(String.format("| %-3s | %-16s |\n", "No", "Column"));
            table.append("----------------------------\n");
            for (int i = 0; i < fields.size(); i++) {
                table.append(String.format("| %02d  | %-16s |\n", i + 1, fields.get(i)));
            }
            table.append("----------------------------");
            return table.toString();
        }
        
        if (type.equals("CONTROLLER")) {
            String summary = classDef.getMethods().stream()
                    .filter(m -> m.isAnnotationPresent("GetMapping") || m.isAnnotationPresent("PostMapping") ||
                                 m.isAnnotationPresent("PutMapping") || m.isAnnotationPresent("PatchMapping") ||
                                 m.isAnnotationPresent("DeleteMapping"))
                    .map(m -> {
                        if (m.isAnnotationPresent("GetMapping")) return "@GET";
                        if (m.isAnnotationPresent("PostMapping")) return "@POST";
                        if (m.isAnnotationPresent("PutMapping")) return "@PUT";
                        if (m.isAnnotationPresent("PatchMapping")) return "@PATCH";
                        if (m.isAnnotationPresent("DeleteMapping")) return "@DELETE";
                        return "";
                    })
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.joining(" "));

            String baseMapping = classDef.getAnnotationByName("RequestMapping")
                    .map(a -> {
                        String s = a.toString();
                        return s.contains("\"") ? s.substring(s.indexOf("\"") + 1, s.lastIndexOf("\"")) : "";
                    }).orElse("");

            String details = classDef.getMethods().stream()
                    .filter(m -> m.isAnnotationPresent("GetMapping") || m.isAnnotationPresent("PostMapping") ||
                                 m.isAnnotationPresent("PutMapping") || m.isAnnotationPresent("PatchMapping") || // ✅ 추가됨
                                 m.isAnnotationPresent("DeleteMapping"))
                    .map(m -> {
                        String methodTag = "";
                        String path = "";
                        if (m.isAnnotationPresent("GetMapping")) { methodTag = "@GET"; path = extractPath(m, "GetMapping"); }
                        else if (m.isAnnotationPresent("PostMapping")) { methodTag = "@POST"; path = extractPath(m, "PostMapping"); }
                        else if (m.isAnnotationPresent("PutMapping")) { methodTag = "@PUT"; path = extractPath(m, "PutMapping"); }
                        else if (m.isAnnotationPresent("PatchMapping")) { methodTag = "@PATCH"; path = extractPath(m, "PatchMapping"); } // ✅ 추가됨
                        else if (m.isAnnotationPresent("DeleteMapping")) { methodTag = "@DELETE"; path = extractPath(m, "DeleteMapping"); }
                        
                        return methodTag + " " + baseMapping + path + " [" + m.getNameAsString() + "]";
                    })
                    .distinct()
                    .collect(Collectors.joining("\n"));

            return summary + "\n---------------------------\n" + details;
        }

        if (type.equals("REPOSITORY") || type.equals("SERVICE")) {
            List<String> methods = classDef.getMethods().stream()
                    .map(m -> m.getNameAsString() + "()")
                    .distinct()
                    .toList();
            String prefix = type.equals("REPOSITORY") ? "" : "Logic: ";
            return methods.isEmpty() ? "No Methods" : prefix + String.join("\n", methods);
        }

        String logic = classDef.getMethods().isEmpty() ? "Standard" : classDef.getMethods().get(0).getNameAsString() + "()";
        return "Logic: " + logic;
    }

    // 클래스 내부의 메서드 호출 및 필드 기반의 의존 관계를 분석하여 Edge 리스트에 추가합니다.
    private void extractAdvancedRelationships(ClassOrInterfaceDeclaration classDef, String className, List<FlowMapResponse.Edge> edges, java.util.Map<String, List<String>> dtoFieldMap) {
        Map<String, String> fieldTypeMap = new java.util.HashMap<>();
        classDef.getFields().forEach(f -> {
            if (f.isStatic() && f.isFinal()) return;
            String type = f.getElementType().asString();
            f.getVariables().forEach(v -> fieldTypeMap.put(v.getNameAsString(), type));
        });

        classDef.getMethods().forEach(method -> {
            String sourceMethodName = method.getNameAsString();

            method.findAll(com.github.javaparser.ast.expr.MethodCallExpr.class).forEach(mce -> {
                mce.getScope().ifPresent(scope -> {
                    String scopeName = scope.toString();
                    String targetMethodName = mce.getNameAsString();

                    if (fieldTypeMap.containsKey(scopeName)) {
                        String targetClassName = fieldTypeMap.get(scopeName);

                        addEdgeWithMethod(className, targetClassName, sourceMethodName, targetMethodName, edges);
                    }
                });
            });
        });
        
        classDef.getFields().forEach(field -> {
            if (field.isStatic() && field.isFinal()) return;
            String target = field.getElementType().asString();
            if (target.endsWith("Service") || target.endsWith("Repository")) {
                Set<String> fieldVarNames = new HashSet<>();
                field.getVariables().forEach(v -> fieldVarNames.add(v.getNameAsString()));
                Set<String> fieldRelatedDtos = new HashSet<>();
                classDef.getMethods().forEach(m -> {
                    boolean usesThisField = m.findAll(com.github.javaparser.ast.expr.MethodCallExpr.class)
                        .stream()
                        .anyMatch(mce -> mce.getScope()
                            .map(s -> fieldVarNames.contains(s.toString()))
                            .orElse(false));
                    if (usesThisField) {
                        m.getParameters().forEach(p -> {
                            String t = extractGenericType(p.getType().asString());
                            if (isDto(t)) fieldRelatedDtos.add(t);
                        });
                        String ret = extractGenericType(m.getType().asString());
                        if (isDto(ret)) fieldRelatedDtos.add(ret);
                    }
                });
                if (fieldRelatedDtos.isEmpty()) return;
                String fieldDtoLabel = String.join(", ", fieldRelatedDtos);
                java.util.Map<String, List<String>> fieldDtoDetails = new java.util.HashMap<>();
                for (String dtoName : fieldRelatedDtos) {
                    if (dtoFieldMap.containsKey(dtoName)) {
                        fieldDtoDetails.put(dtoName, dtoFieldMap.get(dtoName));
                    }
                }
                addEdge(className, target, "DEPENDENCY", fieldDtoLabel, fieldDtoDetails, edges);
            }
            if (field.isAnnotationPresent("OneToMany")) {
                addEdge(className, extractGenericType(target), "ERD", "1:N", null, edges);
            } else if (field.isAnnotationPresent("ManyToOne")) {
                addEdge(className, extractGenericType(target), "ERD", "N:1", null, edges);
            }
        });
    }

    private void addEdgeWithLabel(String source, String target, String label, List<FlowMapResponse.Edge> edges) {
        if (source.equals(target)) return;
        String edgeId = source + "-" + target;
        
        if (edges.stream().noneMatch(e -> e.getId().equals(edgeId))) {
            edges.add(FlowMapResponse.Edge.builder()
                    .id(edgeId)
                    .source(source)
                    .target(target)
                    .label(label)
                    .build());
        }
    }

    // 메서드 단위 에지 생성을 위한 전용 헬퍼
    private void addEdgeWithMethod(String source, String target, String srcMethod, String tgtMethod, List<FlowMapResponse.Edge> edges) {
        if (source.equals(target)) return;

        String edgeId = "CALL-" + source + "-" + target;
        String newCallLabel = srcMethod + " ➔ " + tgtMethod;

        FlowMapResponse.Edge existing = edges.stream()
                .filter(e -> e.getId().equals(edgeId))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            if (!existing.getLabel().contains(newCallLabel)) {
                String mergedLabel = existing.getLabel() + "\n" + newCallLabel;
                edges.remove(existing);
                edges.add(FlowMapResponse.Edge.builder()
                        .id(edgeId)
                        .source(source)
                        .target(target)
                        .sourceMethod(existing.getSourceMethod())
                        .targetMethod(existing.getTargetMethod())
                        .type("METHOD_CALL")
                        .label(mergedLabel)
                        .build());
            }
        } else {
            edges.add(FlowMapResponse.Edge.builder()
                    .id(edgeId)
                    .source(source)
                    .target(target)
                    .sourceMethod(srcMethod)
                    .targetMethod(tgtMethod)
                    .type("METHOD_CALL")
                    .label(newCallLabel)
                    .build());
        }
    }

    private String extractGenericType(String type) {
        if (type.contains("<")) {
            return type.substring(type.indexOf("<") + 1, type.indexOf(">"));
        }
        return type;
    }

    private boolean isDto(String name) {
        return name.endsWith("Dto") || name.endsWith("Request") || name.endsWith("Response") || name.endsWith("VO");
    }

    private void addEdge(String source, String target, String type, String label, java.util.Map<String, List<String>> dtoDetails, List<FlowMapResponse.Edge> edges) {
        if (source.equals(target)) return;
        
        String prefix = type.equals("ERD") ? "ERD-" : "";
        String edgeId = prefix + source + "-" + target;
        
        if (edges.stream().noneMatch(e -> e.getId().equals(edgeId))) {
            edges.add(FlowMapResponse.Edge.builder()
                    .id("DTO-" + source + "-" + target)
                    .source(source)
                    .target(target)
                    .label(label)
                    .type(type)
                    .dtoDetails(dtoDetails)
                    .build());
        }
    }

    // API 경로 추출을 위한 헬퍼 메서드
    private String extractPath(com.github.javaparser.ast.body.MethodDeclaration m, String annotationName) {
        return m.getAnnotationByName(annotationName)
                .map(a -> {
                    String val = a.toString();
                    if (val.contains("\"")) {
                        return val.substring(val.indexOf("\"") + 1, val.lastIndexOf("\""));
                    }
                    return "";
                }).orElse("");
    }

    private String determineType(ClassOrInterfaceDeclaration classDef) {
        String className = classDef.getNameAsString();

        if (classDef.isAnnotationPresent("RestController") || 
            classDef.isAnnotationPresent("Controller") || 
            className.endsWith("Controller")) return "CONTROLLER";
        if (classDef.isAnnotationPresent("Service") || className.endsWith("Service") || className.endsWith("ServiceImpl")) return "SERVICE";
        if (classDef.isAnnotationPresent("Repository") || className.endsWith("Repository")) return "REPOSITORY";
        if (classDef.isAnnotationPresent("Entity") || className.endsWith("Entity")) return "ENTITY";
        if (isDto(classDef.getNameAsString())) return "DTO";
        return "COMPONENT";
    }

    private String getColorByType(String type) {
        switch (type) {
            case "DOMAIN": return "#8b5cf6";
            case "CONTROLLER": return "#6366f1";
            case "SERVICE": return "#10b981";
            case "REPOSITORY": return "#f59e0b";
            case "ENTITY": return "#ec4899";
            case "METHOD_CALL": return "#3b82f6";
            default: return "#94a3b8";
        }
    }
}
