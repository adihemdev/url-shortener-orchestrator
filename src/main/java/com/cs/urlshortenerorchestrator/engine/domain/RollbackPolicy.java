package com.cs.urlshortenerorchestrator.engine.domain;

import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * RollbackPolicy: defines how to revert changes after failure or rejection.
 */
public class RollbackPolicy {
    @Getter
    private final boolean reversible;
    @Getter
    private final List<String> reversibleOperations;
    @Getter
    private final List<String> autoRollbackTriggers;
    @Getter
    private final int timeoutSeconds;

    public RollbackPolicy(boolean isReversible, List<String> reversibleOperations,
                         List<String> autoRollbackTriggers, int timeoutSeconds) {
        this.reversible = isReversible;
        this.reversibleOperations = reversibleOperations != null ? reversibleOperations : List.of();
        this.autoRollbackTriggers = autoRollbackTriggers != null ? autoRollbackTriggers : List.of();
        this.timeoutSeconds = timeoutSeconds;
    }


    public boolean shouldAutoRollback(String triggerType) {
        return autoRollbackTriggers.contains(triggerType);
    }

    public static RollbackPolicy noRollback() {
        return new RollbackPolicy(false, List.of(), List.of(), 0);
    }

    public static RollbackBuilder builder() {
        return new RollbackBuilder();
    }

    public static class RollbackBuilder {
        private boolean isReversible = true;
        private List<String> reversibleOperations = List.of();
        private List<String> autoRollbackTriggers = List.of();
        private int timeoutSeconds = 300;

        public RollbackBuilder reversible(boolean reversible) {
            this.isReversible = reversible;
            return this;
        }

        public RollbackBuilder operations(List<String> ops) {
            this.reversibleOperations = ops;
            return this;
        }

        public RollbackBuilder autoRollbackOn(List<String> triggers) {
            this.autoRollbackTriggers = triggers;
            return this;
        }

        public RollbackBuilder timeout(int seconds) {
            this.timeoutSeconds = seconds;
            return this;
        }

        public RollbackPolicy build() {
            return new RollbackPolicy(isReversible, reversibleOperations,
                autoRollbackTriggers, timeoutSeconds);
        }
    }
}

