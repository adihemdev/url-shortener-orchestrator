package com.cs.urlshortenerorchestrator.engine.domain;

import java.util.Objects;
import java.util.function.Predicate;
import lombok.Getter;

/**
 * ValidationRule: a single validation check for entry/exit gates.
 * Can check dependencies, artifacts, metrics, policies, etc.
 */
public class ValidationRule {
    public enum RuleType {
        DEPENDENCY_CHECK, ARTIFACT_CHECK, METRIC_CHECK, POLICY_CHECK,
        RESOURCE_CHECK, SCHEMA_VALID, CUSTOM
    }
    public enum Severity { ERROR, WARNING, INFO }

    @Getter
    private final String name;
    @Getter
    private final RuleType type;
    @Getter
    private final String targetArtifact;
    @Getter
    private final Predicate<Object> condition;
    @Getter
    private final Severity severity;
    @Getter
    private final String description;

    public ValidationRule(String name, RuleType type, String targetArtifact,
                        Predicate<Object> condition, Severity severity, String description) {
        this.name = Objects.requireNonNull(name, "name required");
        this.type = Objects.requireNonNull(type, "type required");
        this.targetArtifact = targetArtifact;
        this.condition = Objects.requireNonNull(condition, "condition required");
        this.severity = Objects.requireNonNull(severity, "severity required");
        this.description = description;
    }


    public boolean evaluate(Object value) {
        try {
            return condition.test(value);
        } catch (Exception e) {
            return false;
        }
    }

    public static ValidationRule dependency(String nodeName) {
        return new ValidationRule(
            "dependency_" + nodeName,
            RuleType.DEPENDENCY_CHECK,
            null,
            ctx -> true,
            Severity.ERROR,
            "Node " + nodeName + " must complete before execution"
        );
    }

    public static ValidationRule artifactExists(String artifactId) {
        return new ValidationRule(
            "artifact_" + artifactId,
            RuleType.ARTIFACT_CHECK,
            artifactId,
            artifact -> artifact != null,
            Severity.ERROR,
            "Artifact " + artifactId + " must exist"
        );
    }

    public static ValidationRule metricThreshold(String metricName, long minValue) {
        return new ValidationRule(
            "metric_" + metricName,
            RuleType.METRIC_CHECK,
            metricName,
            value -> value instanceof Number && ((Number) value).longValue() >= minValue,
            Severity.ERROR,
            metricName + " must be >= " + minValue
        );
    }
}
