package com.cs.urlshortenerorchestrator.engine.domain;

import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * ReplanTrigger: defines when and how to regenerate the workflow DAG.
 * Triggered when assumptions break or validation fails.
 */
public class ReplanTrigger {
    public enum TriggerType {
        ASSUMPTION_BROKEN, VALIDATION_FAILED, EXPLICIT_REQUEST, REGRESSION_DETECTED
    }

    @Getter
    private final String id;
    @Getter
    private final TriggerType triggerType;
    @Getter
    private final String failureCode;
    @Getter
    private final String nodeIdToReplanFrom;
    @Getter
    private final List<String> assumptionsBroken;
    @Getter
    private final int maxReplans;
    @Getter
    private final String reasonDescription;

    public ReplanTrigger(String id, TriggerType triggerType, String failureCode,
                        String nodeIdToReplanFrom, List<String> assumptionsBroken,
                        int maxReplans, String reasonDescription) {
        this.id = Objects.requireNonNull(id, "id required");
        this.triggerType = Objects.requireNonNull(triggerType, "triggerType required");
        this.failureCode = failureCode;
        this.nodeIdToReplanFrom = Objects.requireNonNull(nodeIdToReplanFrom, "nodeIdToReplanFrom required");
        this.assumptionsBroken = assumptionsBroken != null ? assumptionsBroken : List.of();
        this.maxReplans = maxReplans;
        this.reasonDescription = reasonDescription;
    }


    public static Builder builder(String id, TriggerType type, String nodeIdToReplanFrom) {
        return new Builder(id, type, nodeIdToReplanFrom);
    }

    public static class Builder {
        private final String id;
        private final TriggerType triggerType;
        private final String nodeIdToReplanFrom;
        private String failureCode;
        private List<String> assumptionsBroken = List.of();
        private int maxReplans = 3;
        private String reasonDescription;

        public Builder(String id, TriggerType type, String nodeIdToReplanFrom) {
            this.id = id;
            this.triggerType = type;
            this.nodeIdToReplanFrom = nodeIdToReplanFrom;
        }

        public Builder failureCode(String code) {
            this.failureCode = code;
            return this;
        }

        public Builder assumptionsBroken(List<String> assumptions) {
            this.assumptionsBroken = assumptions;
            return this;
        }

        public Builder maxReplans(int max) {
            this.maxReplans = max;
            return this;
        }

        public Builder reason(String reason) {
            this.reasonDescription = reason;
            return this;
        }

        public ReplanTrigger build() {
            return new ReplanTrigger(id, triggerType, failureCode, nodeIdToReplanFrom,
                assumptionsBroken, maxReplans, reasonDescription);
        }
    }
}
