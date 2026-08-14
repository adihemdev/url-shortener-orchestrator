package com.cs.urlshortenerorchestrator.engine.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class BoundedWorkspaceToolTest {

    @TempDir
    Path projectRoot;

    @Test
    void allowsAgentToWriteWithinAnalyticsWorkspace() {

        BoundedWorkspaceTool workspace =
                new BoundedWorkspaceTool(projectRoot);

        String path =
                "src/main/java/com/cs/urlshortenerorchestrator/" +
                        "analytics/domain/ClickEvent.java";

        workspace.writeFile(
                path,
                "public record ClickEvent() {}"
        );

        assertThat(workspace.exists(path)).isTrue();

        assertThat(workspace.readFile(path))
                .contains("ClickEvent");
    }

    @Test
    void preventsAgentFromModifyingOrchestrationEngine() {

        BoundedWorkspaceTool workspace =
                new BoundedWorkspaceTool(projectRoot);

        assertThatThrownBy(() ->
                workspace.writeFile(
                        "src/main/java/com/cs/urlshortenerorchestrator/" +
                                "engine/execution/WorkflowExecutor.java",
                        "malicious replacement"
                )
        )
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("access denied");
    }

    @Test
    void preventsPathTraversalOutsideAllowedWorkspace() {

        BoundedWorkspaceTool workspace =
                new BoundedWorkspaceTool(projectRoot);

        assertThatThrownBy(() ->
                workspace.writeFile(
                        "src/main/java/com/cs/urlshortenerorchestrator/" +
                                "analytics/../../../../../../pom.xml",
                        "replacement"
                )
        )
                .isInstanceOf(SecurityException.class);
    }
}