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

class LiveBrownfieldTestingAgentTest {

    @Test
    void liveTestingAgentUpdatesExistingTestsForDeleteBehavior() {

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
         * Brownfield testing:
         *
         * READ:
         *   existing target application source + tests
         *
         * WRITE:
         *   existing target application tests only
         */
        BoundedWorkspaceTool workspace =
                new BoundedWorkspaceTool(
                        projectRoot,
                        List.of(
                                "src/main/java/com/cs/urlshortenerorchestrator/targetapp",
                                "src/test/java/com/cs/urlshortenerorchestrator/targetapp"
                        ),
                        List.of(
                                "src/test/java/com/cs/urlshortenerorchestrator/targetapp"
                        )
                );

        AgentClient liveClient =
                new OpenAiCompatibleAgentClient(
                        System.getenv("LLM_BASE_URL"),
                        System.getenv("LLM_API_KEY"),
                        System.getenv("LLM_MODEL"),
                        6000
                );

        EngineeringAgent engineeringAgent =
                new EngineeringAgent(
                        liveClient,
                        workspace,
                        new ObjectMapper()
                );

        List<Artifact> artifacts =
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
                        new Artifact(
                                "brownfield-test-requirements",
                                ArtifactType.DOCUMENTATION,
                                "brownfield-test-requirements",
                                "testing",
                                "live-brownfield-testing",
                                "runtime:brownfield-test-requirements",
                                Map.of(
                                        "newBehavior",
                                        "DELETE /api/v1/urls/{shortCode}",

                                        "success",
                                        "Existing short code returns HTTP 204 and removes the mapping",

                                        "notFound",
                                        "Missing short code returns HTTP 404",

                                        "regression",
                                        "Existing POST shortening and GET redirect behavior must remain covered"
                                ),
                                Instant.now()
                        )
                );

        EngineeringTask task =
                new EngineeringTask(
                        AgentRole.TESTING,
                           """
                                    Update only the existing UrlServiceTests and UrlControllerTests
                                    for the newly added DELETE behavior.
                                    
                                    Required coverage:
                                    
                                    1. UrlServiceTests
                                       - deleting an existing short code returns true
                                       - verifies the repository delete operation
                                       - deleting a missing short code returns false
                                       - verifies no delete operation occurs
                                    
                                    2. UrlControllerTests
                                       - DELETE existing short code returns HTTP 204
                                       - DELETE missing short code returns HTTP 404
                                    
                                    Preserve every existing POST and GET test unchanged.
                                    
                                    Do not modify UrlShortenerIntegrationTests; integration DELETE
                                    coverage already exists.
                                    """,
                        artifacts,
                        List.of(
                                "Use JUnit 5",
                                "Modify test source only",
                                "Do not modify production source code",
                                "Preserve all existing useful POST and GET tests",
                                "Do not introduce new dependencies",
                                "Follow the existing testing style",
                                "Make the smallest necessary brownfield test changes"
                        ),
                        "src/test/java/com/cs/urlshortenerorchestrator/targetapp/"
                );

        CodeGenerationResult result =
                engineeringAgent.execute(task);

        assertThat(result.files())
                .isNotEmpty();

        /*
         * We expect modifications to the existing test suite,
         * not a large generated parallel suite.
         */
        assertThat(result.files().size())
                .isLessThanOrEqualTo(2);

        result.files().forEach(file -> {

            assertThat(file.path())
                    .startsWith(
                            "src/test/java/com/cs/urlshortenerorchestrator/targetapp/"
                    );

            assertThat(workspace.exists(file.path()))
                    .isTrue();
        });

        /*
         * Production source must not be returned or modified by
         * this testing task.
         */
        assertThat(
                result.files()
                        .stream()
                        .map(file -> file.path())
        ).noneMatch(path ->
                path.startsWith("src/main/")
        );

        System.out.println();
        System.out.println(
                "=== LIVE BROWNFIELD TEST GENERATION ==="
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
                "live-brownfield-testing",
                path,
                Map.of(
                        "scenario", "BROWNFIELD",
                        "existing", "true"
                ),
                Instant.now()
        );
    }
}