package com.cs.urlshortenerorchestrator.analytics.orchestration;


import com.cs.urlshortenerorchestrator.engine.agent.*;
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

class LiveBrownfieldImpactAnalysisTest {

    @Test
    void liveAgentAnalyzesDeleteChangeAgainstExistingTargetApp() {

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
                        )
                );

        EngineeringTask task =
                new EngineeringTask(
                        AgentRole.ANALYSIS,
                        """
                        Add support for deleting an existing shortened URL
                        by short code.

                        Return success when the mapping exists and 404 when
                        it does not.

                        Existing POST shortening and GET redirect behavior
                        must remain unchanged.
                        """,
                        existingArtifacts,
                        List.of(
                                "Do not modify any files",
                                "Identify the smallest safe change surface",
                                "Reuse existing Spring Data JPA capabilities where possible",
                                "Preserve existing behavior and regression coverage"
                        ),
                        ""
                );

        ImpactAnalysis analysis =
                engineeringAgent.analyze(task);

        System.out.println();
        System.out.println(
                "=== LIVE BROWNFIELD IMPACT ANALYSIS ==="
        );
        System.out.println(
                "Impacted files: "
                        + analysis.impactedFiles()
        );
        System.out.println(
                "Preserved behavior: "
                        + analysis.preservedBehaviors()
        );
        System.out.println(
                "Implementation steps: "
                        + analysis.implementationSteps()
        );
        System.out.println(
                "Test changes: "
                        + analysis.testChanges()
        );
        System.out.println(
                "Risks: "
                        + analysis.risks()
        );
        System.out.println(
                "Summary: "
                        + analysis.summary()
        );

        assertThat(analysis.impactedFiles())
                .isNotEmpty();

        assertThat(analysis.implementationSteps())
                .isNotEmpty();

        assertThat(analysis.testChanges())
                .isNotEmpty();

        assertThat(analysis.preservedBehaviors())
                .isNotEmpty();
    }

    private Artifact artifact(
            String id,
            ArtifactType type,
            String path) {

        return new Artifact(
                id,
                type,
                path,
                "brownfield-discovery",
                "live-analysis",
                path,
                Map.of(
                        "scenario", "BROWNFIELD",
                        "existing", "true"
                ),
                Instant.now()
        );
    }
}