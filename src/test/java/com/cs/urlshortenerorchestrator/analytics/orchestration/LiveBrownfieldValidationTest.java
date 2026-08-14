package com.cs.urlshortenerorchestrator.analytics.orchestration;

import com.cs.urlshortenerorchestrator.engine.agent.AgentClient;
import com.cs.urlshortenerorchestrator.engine.agent.AgentRole;
import com.cs.urlshortenerorchestrator.engine.agent.BoundedWorkspaceTool;
import com.cs.urlshortenerorchestrator.engine.agent.EngineeringAgent;
import com.cs.urlshortenerorchestrator.engine.agent.EngineeringTask;
import com.cs.urlshortenerorchestrator.engine.agent.OpenAiCompatibleAgentClient;
import com.cs.urlshortenerorchestrator.engine.agent.ValidationAssessment;
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

class LiveBrownfieldValidationTest {

    @Test
    void liveValidatorConfirmsDeleteChangeAndRegressionSafety() {

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

        BoundedWorkspaceTool workspace =
                new BoundedWorkspaceTool(
                        projectRoot,
                        List.of(
                                "src/main/java/com/cs/urlshortenerorchestrator/targetapp",
                                "src/test/java/com/cs/urlshortenerorchestrator/targetapp"
                        ),
                        List.of()
                );

        AgentClient liveClient =
                new OpenAiCompatibleAgentClient(
                        System.getenv("LLM_BASE_URL"),
                        System.getenv("LLM_API_KEY"),
                        System.getenv("LLM_MODEL"),
                        3000
                );

        EngineeringAgent engineeringAgent =
                new EngineeringAgent(
                        liveClient,
                        workspace,
                        new ObjectMapper()
                );

        List<Artifact> evidence =
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
                                "brownfield-test-execution",
                                ArtifactType.TEST,
                                "brownfield-targetapp-test-execution",
                                "testing",
                                "brownfield-validation",
                                "runtime:test-execution",
                                Map.of(
                                        "status", "PASS",
                                        "suite",
                                        "UrlServiceTests, UrlControllerTests, UrlShortenerIntegrationTests",
                                        "result",
                                        "All targeted brownfield and regression tests passed"
                                ),
                                Instant.now()
                        )
                );

        EngineeringTask task =
                new EngineeringTask(
                        AgentRole.VALIDATION,
                        """
                        Validate the brownfield DELETE change against the
                        requested behavior and regression requirements.

                        Required behavior:

                        - DELETE /api/v1/urls/{shortCode} succeeds when the
                          mapping exists
                        - successful DELETE removes the mapping
                        - DELETE returns 404 when the short code does not exist
                        - existing POST shortening behavior remains intact
                        - existing GET redirect behavior remains intact
                        - service, controller, and integration regression tests
                          pass

                        Return PASS only if the implementation and test evidence
                        support all required criteria.
                        """,
                        evidence,
                        List.of(
                                "Do not generate or modify code",
                                "Do not infer behavior without implementation or test evidence",
                                "Treat actual passing test execution as strong evidence",
                                "All required criteria must be supported for PASS"
                        ),
                        ""
                );

        ValidationAssessment assessment =
                engineeringAgent.validate(task);

        System.out.println();
        System.out.println(
                "=== LIVE BROWNFIELD VALIDATION ==="
        );
        System.out.println(
                "Status: " + assessment.status()
        );
        System.out.println(
                "Criteria: " + assessment.criteriaResults()
        );
        System.out.println(
                "Gaps: " + assessment.gaps()
        );
        System.out.println(
                "Summary: " + assessment.summary()
        );

        assertThat(
                assessment.status().name()
        ).isEqualTo("PASS");

        assertThat(
                assessment.gaps()
        ).isEmpty();
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
                "live-brownfield-validation",
                path,
                Map.of(
                        "scenario", "BROWNFIELD",
                        "existing", "true"
                ),
                Instant.now()
        );
    }
}