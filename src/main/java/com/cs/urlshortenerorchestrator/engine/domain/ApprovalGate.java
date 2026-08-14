package com.cs.urlshortenerorchestrator.engine.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * ApprovalGate: explicit human approval checkpoint.
 * Requires human decision before proceeding (with optional auto-approve rules).
 */
public class ApprovalGate {
    @Getter
    private final String gateName;
    @Getter
    private final String description;
    @Getter
    private final boolean required;
    @Getter
    private final List<String> appliesTo;
    @Getter
    private final String defaultApprover;
    @Getter
    private final Predicate<ApprovalContext> autoApproveRule;
    @Getter
    private final int timeoutMinutes;

    public ApprovalGate(String gateName, String description, boolean required,
                       List<String> appliesTo, String defaultApprover,
                       Predicate<ApprovalContext> autoApproveRule, int timeoutMinutes) {
        this.gateName = Objects.requireNonNull(gateName, "gateName required");
        this.description = description;
        this.required = required;
        this.appliesTo = appliesTo != null ? appliesTo : List.of();
        this.defaultApprover = defaultApprover;
        this.autoApproveRule = autoApproveRule;
        this.timeoutMinutes = timeoutMinutes;
    }


    public boolean shouldAutoApprove(ApprovalContext context) {
        return autoApproveRule != null && autoApproveRule.test(context);
    }

    public static Builder builder(String gateName) {
        return new Builder(gateName);
    }

    /**
     * Context for auto-approval decision.
     */
    public static class ApprovalContext {
        private final String nodeId;
        private final Map<String, Object> metrics;
        private final Map<String, Object> artifacts;

        public ApprovalContext(String nodeId, Map<String, Object> metrics, Map<String, Object> artifacts) {
            this.nodeId = nodeId;
            this.metrics = metrics != null ? metrics : Map.of();
            this.artifacts = artifacts != null ? artifacts : Map.of();
        }

        public String getNodeId() { return nodeId; }
        public Map<String, Object> getMetrics() { return metrics; }
        public Map<String, Object> getArtifacts() { return artifacts; }
    }

    public static class Builder {
        private final String gateName;
        private String description;
        private boolean required = true;
        private List<String> appliesTo = List.of();
        private String defaultApprover;
        private Predicate<ApprovalContext> autoApproveRule;
        private int timeoutMinutes = 120;

        public Builder(String gateName) {
            this.gateName = gateName;
        }

        public Builder description(String desc) {
            this.description = desc;
            return this;
        }

        public Builder required(boolean req) {
            this.required = req;
            return this;
        }

        public Builder appliesTo(List<String> nodeTypes) {
            this.appliesTo = nodeTypes;
            return this;
        }

        public Builder approver(String approver) {
            this.defaultApprover = approver;
            return this;
        }

        public Builder autoApproveWhen(Predicate<ApprovalContext> rule) {
            this.autoApproveRule = rule;
            return this;
        }

        public Builder timeoutMinutes(int minutes) {
            this.timeoutMinutes = minutes;
            return this;
        }

        public ApprovalGate build() {
            return new ApprovalGate(gateName, description, required, appliesTo,
                defaultApprover, autoApproveRule, timeoutMinutes);
        }
    }
}

