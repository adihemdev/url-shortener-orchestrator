package com.cs.urlshortenerorchestrator.engine.domain;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Workflow represents a complete SDLC orchestration plan.
 * Contains nodes, edges, execution state, and validation logic.
 */
public class Workflow {
    private final String id;
    private final String name;
    private final String description;
    private final WorkflowNode rootNode;
    private final Map<String, WorkflowNode> nodesById;
    private final WorkflowState currentState;
    private final Instant createdAt;
    private final Map<String, Object> metadata;

    public Workflow(String id, String name, String description, WorkflowNode rootNode,
                   Map<String, WorkflowNode> nodesById, WorkflowState currentState,
                   Instant createdAt, Map<String, Object> metadata) {
        this.id = Objects.requireNonNull(id, "id required");
        this.name = Objects.requireNonNull(name, "name required");
        this.description = description;
        this.rootNode = Objects.requireNonNull(rootNode, "rootNode required");
        this.nodesById = Objects.requireNonNull(nodesById, "nodesById required");
        this.currentState = currentState != null ? currentState : new WorkflowState(rootNode.getId());
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public WorkflowNode getRootNode() { return rootNode; }
    public Map<String, WorkflowNode> getNodesById() { return nodesById; }
    public WorkflowState getCurrentState() { return currentState; }
    public Instant getCreatedAt() { return createdAt; }
    public Map<String, Object> getMetadata() { return metadata; }

    // ...existing code...

    /**
     * Validate workflow DAG structure (acyclic, consistent).
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        // Check for cycles
        if (!isAcyclic()) {
            errors.add("Workflow contains cycles");
        }

        // Check all referenced nodes exist
        for (WorkflowNode node : nodesById.values()) {
            for (String dep : node.getDependsOnNodeIds()) {
                if (!nodesById.containsKey(dep)) {
                    errors.add("Node " + node.getId() + " depends on unknown node " + dep);
                }
            }
            for (String par : node.getParallelNodeIds()) {
                if (!nodesById.containsKey(par)) {
                    errors.add("Node " + node.getId() + " parallels unknown node " + par);
                }
            }
        }

        // Check root node has no dependencies
        if (!rootNode.getDependsOnNodeIds().isEmpty()) {
            errors.add("Root node should have no dependencies");
        }

        return errors;
    }

    /**
     * Check if DAG is acyclic (no circular dependencies).
     */
    private boolean isAcyclic() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String nodeId : nodesById.keySet()) {
            if (!visited.contains(nodeId)) {
                if (hasCycle(nodeId, visited, recursionStack)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean hasCycle(String nodeId, Set<String> visited, Set<String> recursionStack) {
        visited.add(nodeId);
        recursionStack.add(nodeId);

        WorkflowNode node = nodesById.get(nodeId);
        if (node != null) {
            for (String dep : node.getDependsOnNodeIds()) {
                if (!visited.contains(dep)) {
                    if (hasCycle(dep, visited, recursionStack)) {
                        return true;
                    }
                } else if (recursionStack.contains(dep)) {
                    return true;
                }
            }
        }

        recursionStack.remove(nodeId);
        return false;
    }

    /**
     * Get all nodes that can run in parallel after given node.
     */
    public Set<String> getParallelNodes(String nodeId) {
        WorkflowNode node = nodesById.get(nodeId);
        if (node == null) return Set.of();
        return node.getParallelNodeIds();
    }

    /**
     * Get nodes ready to execute (all dependencies complete).
     */
    public Set<String> getReadyNodes() {
        Set<String> ready = new HashSet<>();
        for (WorkflowNode node : nodesById.values()) {
            if (node.getDependsOnNodeIds().isEmpty() ||
                currentState.getCompletedNodeIds().containsAll(node.getDependsOnNodeIds())) {
                if (!currentState.getCompletedNodeIds().contains(node.getId()) &&
                    !currentState.getFailedNodeIds().contains(node.getId())) {
                    ready.add(node.getId());
                }
            }
        }
        return ready;
    }

    public static Builder builder(String id, String name) {
        return new Builder(id, name);
    }

    public static class Builder {
        private final String id;
        private final String name;
        private String description;
        private WorkflowNode rootNode;
        private final Map<String, WorkflowNode> nodesById = new HashMap<>();
        private Map<String, Object> metadata = new HashMap<>();

        public Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder description(String desc) {
            this.description = desc;
            return this;
        }

        public Builder root(WorkflowNode root) {
            this.rootNode = root;
            this.nodesById.put(root.getId(), root);
            return this;
        }

        public Builder node(WorkflowNode node) {
            this.nodesById.put(node.getId(), node);
            return this;
        }

        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Workflow build() {
            if (rootNode == null) {
                throw new IllegalStateException("Root node required");
            }

            Workflow workflow = new Workflow(id, name, description, rootNode,
                new HashMap<>(nodesById), null, Instant.now(), new HashMap<>(metadata));

            // Validate
            List<String> errors = workflow.validate();
            if (!errors.isEmpty()) {
                throw new IllegalStateException("Workflow validation failed: " + errors);
            }

            return workflow;
        }
    }
}

