package com.cs.urlshortenerorchestrator.analytics.orchestration;

import com.cs.urlshortenerorchestrator.engine.agent.AgentClient;
import com.cs.urlshortenerorchestrator.engine.agent.AgentRole;
import com.cs.urlshortenerorchestrator.engine.agent.BoundedWorkspaceTool;
import com.cs.urlshortenerorchestrator.engine.agent.CodeGenerationResult;
import com.cs.urlshortenerorchestrator.engine.agent.EngineeringAgent;
import com.cs.urlshortenerorchestrator.engine.agent.EngineeringTask;
import com.cs.urlshortenerorchestrator.engine.agent.OpenAiCompatibleAgentClient;
import com.cs.urlshortenerorchestrator.engine.domain.Artifact;
import com.cs.urlshortenerorchestrator.engine.domain.ArtifactType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LiveBrownfieldImplementationTest {

    @Test
    void liveAgentMakesBoundedDeleteChangeToExistingTargetApp() {

        assumeTrue(
                "true".equalsIgnoreCase(
                        System.getenv("LIVE_AGENT_TESTS")
                ),
                "Set LIVE_AGENT_TESTS=true to run live agent tests"
        );

        assumeTrue(
                System.getenv("LLM_BASE_URL") != null
                        && System.getenv("LLM_MODEL") != null
                        && System.getenv("LLM_API_KEY") != null,
                "Live LLM configuration is required"
        );

        Path projectRoot =
                Path.of(System.getProperty("user.dir"));

        /*
         * Brownfield implementation:
         *
         * READ:
         *   existing target application source + tests
         *
         * WRITE:
         *   target application production source only
         */
        BoundedWorkspaceTool workspace =
                new BoundedWorkspaceTool(
                        projectRoot,
                        List.of(
                                "src/main/java/com/cs/urlshortenerorchestrator/targetapp",
                                "src/test/java/com/cs/urlshortenerorchestrator/targetapp"
                        ),
                        List.of(
                                "src/main/java/com/cs/urlshortenerorchestrator/targetapp"
                        )
                );

        AgentClient liveClient =
                new OpenAiCompatibleAgentClient(
                        System.getenv("LLM_BASE_URL"),
                        System.getenv("LLM_API_KEY"),
                        System.getenv("LLM_MODEL"),
                        5000
                );

        EngineeringAgent engineeringAgent =
                new EngineeringAgent(
                        liveClient,
                        workspace,
                        new ObjectMapper()
                );

        List<Artifact> existingArtifacts =
                List.of(
                        artifact(
                                "url-controller",
                                ArtifactType.CODE,
                                "src/main/java/com/cs/urlshortenerorchestrator/targetapp/controller/UrlController.java"
                        ),
                        artifact(
                                "url-service",
                                ArtifactType.CODE,
                                "src/main/java/com/cs/urlshortenerorchestrator/targetapp/service/UrlService.java"
                        ),
                        artifact(
                                "url-repository",
                                ArtifactType.CODE,
                                "src/main/java/com/cs/urlshortenerorchestrator/targetapp/repository/UrlRepository.java"
                        ),
                        artifact(
                                "url-mapping",
                                ArtifactType.CODE,
                                "src/main/java/com/cs/urlshortenerorchestrator/targetapp/model/UrlMapping.java"
                        ),
                        artifact(
                                "controller-tests",
                                ArtifactType.TEST,
                                "src/test/java/com/cs/urlshortenerorchestrator/targetapp/controller/UrlControllerTests.java"
                        ),
                        artifact(
                                "service-tests",
                                ArtifactType.TEST,
                                "src/test/java/com/cs/urlshortenerorchestrator/targetapp/service/UrlServiceTests.java"
                        ),
                        artifact(
                                "integration-tests",
                                ArtifactType.TEST,
                                "src/test/java/com/cs/urlshortenerorchestrator/targetapp/UrlShortenerIntegrationTests.java"
                        ),
                        new Artifact(
                                "brownfield-impact-analysis",
                                ArtifactType.DOCUMENTATION,
                                "brownfield-impact-analysis",
                                "analysis",
                                "live-analysis",
                                "runtime:brownfield-impact-analysis",
                                Map.of(
                                        "changeRequest",
                                        "Delete an existing shortened URL by short code",

                                        "expectedImpact",
                                        "UrlController and UrlService; repository only if necessary",

                                        "preserve",
                                        "Existing POST shortening and GET redirect behavior",

                                        "testImpact",
                                        "Add delete success/not-found coverage and preserve existing regression behavior"
                                ),
                                Instant.now()
                        )
                );

        EngineeringTask task =
                new EngineeringTask(
                        AgentRole.IMPLEMENTATION,
                        """
                        Modify the existing URL-shortener application to support
                        deleting an existing shortened URL by short code.

                        Required behavior:

                        - Add DELETE /api/v1/urls/{shortCode}
                        - Return a successful response when the mapping exists
                        - Return 404 when the short code does not exist
                        - Existing POST shortening behavior must remain unchanged
                        - Existing GET redirect behavior must remain unchanged

                        Make the smallest safe brownfield change.

                        Inspect and preserve the existing implementation style.
                        Reuse existing Spring Data JPA capabilities where possible.
                        """,
                        existingArtifacts,
                        List.of(
                                "Modify only production files genuinely required for the change",
                                "Do not modify test files in this implementation step",
                                "Preserve existing POST and GET behavior",
                                "Do not introduce new dependencies",
                                "Do not modify the orchestration engine",
                                "Do not redesign unrelated application components",
                                "Prefer minimal edits to existing classes over new abstractions"
                        ),
                        "src/main/java/com/cs/urlshortenerorchestrator/targetapp/"
                );

        CodeGenerationResult result =
                engineeringAgent.execute(task);

        assertThat(result.files())
                .isNotEmpty();

        /*
         * Brownfield modification should remain small.
         */
        assertThat(result.files().size())
                .isLessThanOrEqualTo(4);

        /*
         * Every generated modification must stay inside the
         * production target-app boundary.
         */
        result.files().forEach(file -> {

            assertThat(file.path())
                    .startsWith(
                            "src/main/java/com/cs/urlshortenerorchestrator/targetapp/"
                    );

            assertThat(workspace.exists(file.path()))
                    .isTrue();
        });

        /*
         * At least the controller or service should need modification.
         */
        assertThat(
                result.files()
                        .stream()
                        .map(file -> file.path())
        ).anyMatch(path ->
                path.endsWith("UrlController.java")
                        || path.endsWith("UrlService.java")
        );

        /*
         * We explicitly did not give the implementation agent
         * write access to src/test/java.
         */
        assertThat(
                result.files()
                        .stream()
                        .map(file -> file.path())
        ).noneMatch(path ->
                path.startsWith("src/test/")
        );

        System.out.println();
        System.out.println(
                "=== LIVE BROWNFIELD IMPLEMENTATION ==="
        );

        result.files().forEach(file ->
                System.out.println(
                        "Modified: " + file.path()
                )
        );

        System.out.println(
                "Summary: " + result.summary()
        );
    }

    private Artifact artifact(
            String id,
            ArtifactType type,
            String path) {

        return new Artifact(
                id,
                type,
                path,
                "brownfield-existing-code",
                "live-brownfield-implementation",
                path,
                Map.of(
                        "scenario", "BROWNFIELD",
                        "existing", "true"
                ),
                Instant.now()
        );
    }
}