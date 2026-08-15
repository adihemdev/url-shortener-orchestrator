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

class EngineeringAgentAmbiguityTest {

    @TempDir
    Path projectRoot;

    @Test
    void blocksImplementationWhenRequirementIsMateriallyAmbiguous()
            throws Exception {

        String controllerPath =
                "src/main/java/com/cs/urlshortenerorchestrator/targetapp/controller/UrlController.java";

        String modelPath =
                "src/main/java/com/cs/urlshortenerorchestrator/targetapp/model/UrlMapping.java";

        writeFile(
                controllerPath,
                """
                package com.cs.urlshortenerorchestrator.targetapp.controller;

                public class UrlController {
                    // Existing create, redirect and delete behavior.
                }
                """
        );

        writeFile(
                modelPath,
                """
                package com.cs.urlshortenerorchestrator.targetapp.model;

                public class UrlMapping {
                    private java.time.Instant expiresAt;
                }
                """
        );

        BoundedWorkspaceTool workspace =
                new BoundedWorkspaceTool(
                        projectRoot,
                        List.of(
                                "src/main/java/com/cs/urlshortenerorchestrator/targetapp"
                        ),
                        List.of()
                );

        AgentClient fakeClient =
                (systemPrompt, userPrompt) -> {

                    assertThat(systemPrompt)
                            .contains(
                                    "sufficiently specified"
                            );

                    assertThat(userPrompt)
                            .contains(
                                    "more control over their shortened URLs"
                            );

                    return new AgentResponse(
                            """
                            {
                              "sufficientlySpecified": false,
                              "implementationBlocked": true,
                              "knownFacts": [
                                "The existing application already supports basic shortened URL operations",
                                "UrlMapping contains an expiresAt field"
                              ],
                              "ambiguities": [
                                "The requested additional user controls are not defined"
                              ],
                              "clarificationQuestions": [
                                "Which additional management capabilities should users have?"
                              ],
                              "unsafeAssumptions": [
                                "Assuming that more control specifically means expiration management"
                              ],
                              "summary":
                                "The request is too ambiguous to implement safely without stakeholder clarification."
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

        EngineeringTask task =
                new EngineeringTask(
                        AgentRole.ANALYSIS,
                        """
                        Users need more control over their shortened URLs.
                        Add the necessary support without breaking existing behavior.
                        """,
                        List.of(
                                artifact(
                                        "controller",
                                        controllerPath
                                ),
                                artifact(
                                        "url-mapping",
                                        modelPath
                                )
                        ),
                        List.of(
                                "Do not modify files",
                                "Do not invent missing product requirements"
                        ),
                        ""
                );

        AmbiguityAssessment assessment =
                engineeringAgent.assessAmbiguity(task);

        assertThat(
                assessment.sufficientlySpecified()
        ).isFalse();

        assertThat(
                assessment.implementationBlocked()
        ).isTrue();

        assertThat(
                assessment.ambiguities()
        ).isNotEmpty();

        assertThat(
                assessment.clarificationQuestions()
        ).isNotEmpty();

        assertThat(
                assessment.unsafeAssumptions()
        ).isNotEmpty();
    }

    @Test
    void allowsImplementationAfterMaterialAmbiguityIsResolved()
            throws Exception {

        String controllerPath =
                "src/main/java/com/cs/urlshortenerorchestrator/targetapp/controller/UrlController.java";

        writeFile(
                controllerPath,
                """
                package com.cs.urlshortenerorchestrator.targetapp.controller;
    
                public class UrlController {
                    // Existing create, redirect and delete behavior.
                }
                """
        );

        BoundedWorkspaceTool workspace =
                new BoundedWorkspaceTool(
                        projectRoot,
                        List.of(
                                "src/main/java/com/cs/urlshortenerorchestrator/targetapp"
                        ),
                        List.of()
                );

        AgentClient fakeClient =
                (systemPrompt, userPrompt) -> {

                    assertThat(userPrompt)
                            .contains("list their shortened URLs");

                    assertThat(userPrompt)
                            .contains("No authentication or ownership changes");

                    return new AgentResponse(
                            """
                            {
                              "sufficientlySpecified": true,
                              "implementationBlocked": false,
                              "knownFacts": [
                                "The application already supports creating, resolving and deleting shortened URLs"
                              ],
                              "ambiguities": [],
                              "clarificationQuestions": [],
                              "unsafeAssumptions": [],
                              "summary":
                                "The clarified request is sufficiently specified for implementation planning."
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

        EngineeringTask task =
                new EngineeringTask(
                        AgentRole.ANALYSIS,
                        """
                        Users need more control over their shortened URLs.
    
                        Stakeholder clarification:
                        - Add the ability to list their shortened URLs.
                        - Return short code, destination URL and creation time.
                        - Do not add editing or expiration behavior.
                        - No authentication or ownership changes are in scope.
                        - Preserve existing POST, GET and DELETE behavior.
                        """,
                        List.of(
                                artifact(
                                        "controller",
                                        controllerPath
                                )
                        ),
                        List.of(
                                "Do not modify files",
                                "Do not invent additional requirements"
                        ),
                        ""
                );

        AmbiguityAssessment assessment =
                engineeringAgent.assessAmbiguity(task);

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

    private void writeFile(
            String relativePath,
            String content)
            throws Exception {

        Path path =
                projectRoot.resolve(relativePath);

        Files.createDirectories(
                path.getParent()
        );

        Files.writeString(
                path,
                content
        );
    }

    private Artifact artifact(
            String id,
            String path) {

        return new Artifact(
                id,
                ArtifactType.CODE,
                path,
                "ambiguity-analysis",
                "ambiguity-test",
                path,
                Map.of(
                        "scenario", "AMBIGUOUS"
                ),
                Instant.now()
        );
    }
}