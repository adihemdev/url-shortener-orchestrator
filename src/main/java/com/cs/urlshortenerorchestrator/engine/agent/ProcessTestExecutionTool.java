package com.cs.urlshortenerorchestrator.engine.agent;

import com.cs.urlshortenerorchestrator.engine.domain.ValidationResult;
import com.cs.urlshortenerorchestrator.engine.domain.ValidationStatus;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class ProcessTestExecutionTool implements TestExecutionTool {

    private final Path projectRoot;

    public ProcessTestExecutionTool(Path projectRoot) {
        this.projectRoot = Objects.requireNonNull(
                projectRoot,
                "projectRoot required"
        ).toAbsolutePath().normalize();
    }

    @Override
    public ValidationResult runTests(
            String nodeId,
            TestExecutionRequest request) {

        Objects.requireNonNull(nodeId, "nodeId required");
        Objects.requireNonNull(request, "request required");

        if (request.command() == null || request.command().isEmpty()) {
            throw new IllegalArgumentException(
                    "Test execution command required"
            );
        }

        ProcessBuilder processBuilder =
                new ProcessBuilder(request.command());

        processBuilder.directory(projectRoot.toFile());
        processBuilder.redirectErrorStream(true);

        StringBuilder output = new StringBuilder();

        try {
            Process process = processBuilder.start();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         process.getInputStream()))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    output.append(line)
                            .append(System.lineSeparator());
                }
            }

            int exitCode = process.waitFor();

            boolean passed = exitCode == 0;

            int targetCount =
                    request.testTargets() == null
                            ? 0
                            : request.testTargets().size();

            return new ValidationResult(
                    "test-execution-" + System.currentTimeMillis(),
                    nodeId,
                    targetCount,
                    passed ? targetCount : 0,
                    passed ? 0 : targetCount,
                    List.of(),
                    passed
                            ? ValidationStatus.PASS
                            : ValidationStatus.FAIL,
                    passed ? null : output.toString(),
                    Instant.now()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return errorResult(nodeId, request, e);

        } catch (Exception e) {
            return errorResult(nodeId, request, e);
        }
    }

    private ValidationResult errorResult(
            String nodeId,
            TestExecutionRequest request,
            Exception exception) {

        int targetCount =
                request.testTargets() == null
                        ? 0
                        : request.testTargets().size();

        return new ValidationResult(
                "test-execution-" + System.currentTimeMillis(),
                nodeId,
                targetCount,
                0,
                targetCount,
                List.of(),
                ValidationStatus.ERROR,
                exception.getClass().getSimpleName()
                        + ": "
                        + exception.getMessage(),
                Instant.now()
        );
    }
}