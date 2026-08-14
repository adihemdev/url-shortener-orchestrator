package com.cs.urlshortenerorchestrator.engine.domain;

import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * Gate: entry/exit conditions for node execution with validation rules.
 * Can be PASS_THROUGH (no checks) or VALIDATION (enforce rules).
 */
public class Gate {
    public enum Type { PASS_THROUGH, VALIDATION, BLOCKING }
    public enum FailureAction { BLOCK, WARN, TRIGGER_REPLAN }

    private final String name;
    private final Type type;
    private final List<ValidationRule> validationRules;
    private final FailureAction failureAction;
    private final String description;

    public Gate(String name, Type type, List<ValidationRule> validationRules,
                FailureAction failureAction, String description) {
        this.name = Objects.requireNonNull(name, "name required");
        this.type = Objects.requireNonNull(type, "type required");
        this.validationRules = validationRules != null ? validationRules : List.of();
        this.failureAction = Objects.requireNonNull(failureAction, "failureAction required");
        this.description = description;
    }

    // Getters
    public String getName() { return name; }
    public Type getType() { return type; }
    public List<ValidationRule> getValidationRules() { return validationRules; }
    public FailureAction getFailureAction() { return failureAction; }
    public String getDescription() { return description; }

    public static Gate passThrough(String name) {
        return new Gate(name, Type.PASS_THROUGH, List.of(), FailureAction.WARN, null);
    }

    public static GateBuilder builder(String name) {
        return new GateBuilder(name);
    }

    public static class GateBuilder {
        private final String name;
        private Type type = Type.VALIDATION;
        private List<ValidationRule> validationRules = List.of();
        private FailureAction failureAction = FailureAction.BLOCK;
        private String description;

        public GateBuilder(String name) {
            this.name = name;
        }

        public GateBuilder passThrough() {
            this.type = Type.PASS_THROUGH;
            return this;
        }

        public GateBuilder validation() {
            this.type = Type.VALIDATION;
            return this;
        }

        public GateBuilder validationRules(List<ValidationRule> rules) {
            this.validationRules = rules;
            return this;
        }

        public GateBuilder failureAction(FailureAction action) {
            this.failureAction = action;
            return this;
        }

        public GateBuilder description(String desc) {
            this.description = desc;
            return this;
        }

        public Gate build() {
            return new Gate(name, type, validationRules, failureAction, description);
        }
    }
}
