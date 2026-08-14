package com.cs.urlshortenerorchestrator.engine.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Filesystem capability exposed to coding agents.
 *
 * Agent access is restricted to explicitly allowed source roots.
 * The agent cannot modify the orchestration engine, target application,
 * build configuration, or repository metadata.
 */
public class BoundedWorkspaceTool implements WorkspaceTool {

    private final Path projectRoot;
    private final List<Path> allowedRoots;

    public BoundedWorkspaceTool(Path projectRoot) {
        this.projectRoot =
                Objects.requireNonNull(projectRoot, "projectRoot required")
                        .toAbsolutePath()
                        .normalize();

        this.allowedRoots = List.of(
                this.projectRoot.resolve(
                        "src/main/java/com/cs/urlshortenerorchestrator/analytics"
                ).normalize(),
                this.projectRoot.resolve(
                        "src/test/java/com/cs/urlshortenerorchestrator/analytics"
                ).normalize()
        );
    }

    @Override
    public String readFile(String relativePath) {
        Path path = resolveAllowed(relativePath);

        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to read workspace file: " + relativePath,
                    e
            );
        }
    }

    @Override
    public void writeFile(String relativePath, String content) {
        Objects.requireNonNull(content, "content required");

        Path path = resolveAllowed(relativePath);

        try {
            Path parent = path.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(path, content);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to write workspace file: " + relativePath,
                    e
            );
        }
    }

    @Override
    public boolean exists(String relativePath) {
        return Files.exists(resolveAllowed(relativePath));
    }

    private Path resolveAllowed(String relativePath) {

        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException(
                    "relativePath required"
            );
        }

        Path resolved =
                projectRoot.resolve(relativePath)
                        .toAbsolutePath()
                        .normalize();

        boolean allowed = allowedRoots.stream()
                .anyMatch(resolved::startsWith);

        if (!allowed) {
            throw new SecurityException(
                    "Agent workspace access denied: " + relativePath
            );
        }

        return resolved;
    }
}