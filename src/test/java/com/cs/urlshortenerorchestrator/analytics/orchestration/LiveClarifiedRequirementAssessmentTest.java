package com.cs.urlshortenerorchestrator.analytics.orchestration;

import com.cs.urlshortenerorchestrator.engine.agent.AgentClient;
import com.cs.urlshortenerorchestrator.engine.agent.AgentRole;
import com.cs.urlshortenerorchestrator.engine.agent.AmbiguityAssessment;
import com.cs.urlshortenerorchestrator.engine.agent.BoundedWorkspaceTool;
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

class LiveClarifiedRequirementAssessmentTest {

    @Test
    void liveAgentUnblocksRequirementAfterStakeholderClarification() {

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
                        )
                );

        EngineeringTask task =
                new EngineeringTask(
                        AgentRole.ANALYSIS,
                        """
                        Original request:

                        Users need more control over their shortened URLs.
                        Add the necessary support without breaking existing behavior.

                        Stakeholder clarification:
                        
                        - Add GET /api/v1/urls to list shortened URLs.
                        - POST /api/v1/urls must continue to create shortened URLs unchanged.
                        - Each list item should use the existing UrlResponse structure:
                          shortCode, longUrl and createdAt.
                        - Return HTTP 200 with an empty JSON array when no URLs exist.
                        - Pagination and filtering are not required.
                        - Return URLs ordered by createdAt descending, newest first.
                        - Editing URLs is not in scope.
                        - Expiration changes are not in scope.
                        - Authentication and ownership changes are not in scope.
                        - Preserve existing POST, redirect GET, and DELETE behavior.
                        """,
                        existingArtifacts,
                        List.of(
                                "Do not modify any files",
                                "Do not invent requirements beyond the stakeholder clarification",
                                "Determine whether implementation can now safely proceed"
                        ),
                        ""
                );

        AmbiguityAssessment assessment =
                engineeringAgent.assessAmbiguity(task);

        System.out.println();
        System.out.println(
                "=== LIVE CLARIFIED REQUIREMENT ASSESSMENT ==="
        );
        System.out.println(
                "Sufficiently specified: "
                        + assessment.sufficientlySpecified()
        );
        System.out.println(
                "Implementation blocked: "
                        + assessment.implementationBlocked()
        );
        System.out.println(
                "Remaining ambiguities: "
                        + assessment.ambiguities()
        );
        System.out.println(
                "Clarification questions: "
                        + assessment.clarificationQuestions()
        );
        System.out.println(
                "Summary: "
                        + assessment.summary()
        );

        assertThat(
                assessment.sufficientlySpecified()
        ).isTrue();

        assertThat(
                assessment.implementationBlocked()
        ).isFalse();

        assertThat(
                assessment.ambiguities()
        ).isEmpty();

        assertThat(
                assessment.clarificationQuestions()
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
                "clarified-requirement-analysis",
                "live-clarification",
                path,
                Map.of(
                        "scenario", "AMBIGUOUS",
                        "existing", "true"
                ),
                Instant.now()
        );
    }
}