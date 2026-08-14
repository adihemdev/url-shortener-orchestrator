package com.cs.urlshortenerorchestrator.engine.agent;

import com.cs.urlshortenerorchestrator.engine.domain.ValidationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessTestExecutionToolTest {

    @TempDir
    Path projectRoot;

    @Test
    void returnsPassWhenCommandSucceeds() {

        ProcessTestExecutionTool tool =
                new ProcessTestExecutionTool(projectRoot);

        TestExecutionRequest request =
                new TestExecutionRequest(
                        List.of("sh", "-c", "exit 0"),
                        List.of("ExampleTest")
                );

        var result =
                tool.runTests("testing", request);

        assertThat(result.status())
                .isEqualTo(ValidationStatus.PASS);

        assertThat(result.totalTests())
                .isEqualTo(1);

        assertThat(result.passedTests())
                .isEqualTo(1);

        assertThat(result.failedTests())
                .isZero();
    }

    @Test
    void returnsFailWhenCommandFails() {

        ProcessTestExecutionTool tool =
                new ProcessTestExecutionTool(projectRoot);

        TestExecutionRequest request =
                new TestExecutionRequest(
                        List.of("sh", "-c", "echo failure && exit 1"),
                        List.of("ExampleTest")
                );

        var result =
                tool.runTests("testing", request);

        assertThat(result.status())
                .isEqualTo(ValidationStatus.FAIL);

        assertThat(result.failedTests())
                .isEqualTo(1);

        assertThat(result.error())
                .contains("failure");
    }
}