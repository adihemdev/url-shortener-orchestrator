package com.cs.urlshortenerorchestrator.engine.agent;

import com.cs.urlshortenerorchestrator.engine.domain.Artifact;
import com.cs.urlshortenerorchestrator.engine.domain.ArtifactType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EngineeringAgentAnalysisTest {

    @TempDir
    Path projectRoot;

    @Test
    void analyzesExistingCodeWithoutWritingFiles()
            throws Exception {

        Path sourceRoot =
                projectRoot.resolve(
                        "src/main/java/com/cs/urlshortenerorchestrator/targetapp/service"
                );

        Path testRoot =
                projectRoot.resolve(
                        "src/test/java/com/cs/urlshortenerorchestrator/targetapp/service"
                );

        Files.createDirectories(sourceRoot);
        Files.createDirectories(testRoot);

        Files.writeString(
                sourceRoot.resolve("UrlService.java"),
                """
                package com.cs.urlshortenerorchestrator.targetapp.service;

                public class UrlService {
                    public String getLongUrl(String shortCode) {
                        return shortCode;
                    }
                }
                """
        );

        Files.writeString(
                testRoot.resolve("UrlServiceTests.java"),
                """
                package com.cs.urlshortenerorchestrator.targetapp.service;

                class UrlServiceTests {
                }
                """
        );

        BoundedWorkspaceTool workspace =
                new BoundedWorkspaceTool(
                        projectRoot,
                        List.of(
                                "src/main/java/com/cs/urlshortenerorchestrator/targetapp",
                                "src/test/java/com/cs/urlshortenerorchestrator/targetapp"
                        ),
                        List.of()
                );

        AgentClient fakeClient =
                (systemPrompt, userPrompt) -> {

                    assertThat(systemPrompt)
                            .contains(
                                    "brownfield impact analysis"
                            );

                    assertThat(userPrompt)
                            .contains(
                                    "UrlService"
                            );

                    assertThat(userPrompt)
                            .contains(
                                    "delete"
                            );

                    return new AgentResponse(
                            """
                            {
                              "impactedFiles": [
                                "src/main/java/com/cs/urlshortenerorchestrator/targetapp/service/UrlService.java",
                                "src/test/java/com/cs/urlshortenerorchestrator/targetapp/service/UrlServiceTests.java"
                              ],
                              "preservedBehaviors": [
                                "Existing URL lookup behavior must remain unchanged"
                              ],
                              "implementationSteps": [
                                "Add minimal delete-by-short-code behavior to UrlService"
                              ],
                              "testChanges": [
                                "Add delete success and missing-code tests"
                              ],
                              "risks": [
                                "Existing lookup behavior could regress"
                              ],
                              "summary": "Delete support requires a small service and test change."
                            }
                            """,
                            "test-model",
                            100,
                            100
                    );
                };

        EngineeringAgent engineeringAgent =
                new EngineeringAgent(
                        fakeClient,
                        workspace,
                        new ObjectMapper()
                );

        List<Artifact> existingArtifacts =
                List.of(
                        artifact(
                                "service",
                                ArtifactType.CODE,
                                "src/main/java/com/cs/urlshortenerorchestrator/targetapp/service/UrlService.java"
                        ),
                        artifact(
                                "service-test",
                                ArtifactType.TEST,
                                "src/test/java/com/cs/urlshortenerorchestrator/targetapp/service/UrlServiceTests.java"
                        )
                );

        EngineeringTask task =
                new EngineeringTask(
                        AgentRole.ANALYSIS,
                        """
                        Add the ability to delete an existing shortened URL
                        by short code. Existing shorten and redirect behavior
                        must remain unchanged.
                        """,
                        existingArtifacts,
                        List.of(
                                "Do not modify files",
                                "Identify the smallest safe change surface",
                                "Reuse existing repository/framework behavior where possible"
                        ),
                        ""
                );

        ImpactAnalysis analysis =
                engineeringAgent.analyze(task);

        assertThat(
                analysis.impactedFiles()
        ).contains(
                "src/main/java/com/cs/urlshortenerorchestrator/targetapp/service/UrlService.java"
        );

        assertThat(
                analysis.preservedBehaviors()
        ).contains(
                "Existing URL lookup behavior must remain unchanged"
        );

        assertThat(
                analysis.testChanges()
        ).isNotEmpty();

        /*
         * The analysis workspace is deliberately read-only.
         */
        assertThatThrownBy(
                () -> workspace.writeFile(
                        "src/main/java/com/cs/urlshortenerorchestrator/targetapp/service/IllegalChange.java",
                        "class IllegalChange {}"
                )
        )
                .isInstanceOf(SecurityException.class);
    }

    private Artifact artifact(
            String id,
            ArtifactType type,
            String path) {

        return new Artifact(
                id,
                type,
                path,
                "brownfield-analysis",
                "analysis-execution",
                path,
                Map.of(
                        "scenario", "BROWNFIELD"
                ),
                Instant.now()
        );
    }
}