package com.cs.urlshortenerorchestrator.engine.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Filesystem capability exposed to engineering agents.
 *
 * Read and write access are independently bounded so brownfield
 * analysis can inspect existing code without automatically gaining
 * permission to modify it.
 */
public class BoundedWorkspaceTool implements WorkspaceTool {

    private final Path projectRoot;
    private final List<Path> allowedReadRoots;
    private final List<Path> allowedWriteRoots;

    /**
     * Default greenfield workspace.
     *
     * Preserves the existing behavior:
     * Analytics source/test files may be read and written.
     */
    public BoundedWorkspaceTool(Path projectRoot) {
        this(
                projectRoot,
                List.of(
                        "src/main/java/com/cs/urlshortenerorchestrator/analytics",
                        "src/test/java/com/cs/urlshortenerorchestrator/analytics"
                ),
                List.of(
                        "src/main/java/com/cs/urlshortenerorchestrator/analytics",
                        "src/test/java/com/cs/urlshortenerorchestrator/analytics"
                )
        );
    }

    /**
     * Configurable bounded workspace.
     *
     * Paths are repository-relative roots.
     */
    public BoundedWorkspaceTool(
            Path projectRoot,
            List<String> readableRoots,
            List<String> writableRoots) {

        this.projectRoot =
                Objects.requireNonNull(
                                projectRoot,
                                "projectRoot required"
                        )
                        .toAbsolutePath()
                        .normalize();

        Objects.requireNonNull(
                readableRoots,
                "readableRoots required"
        );

        Objects.requireNonNull(
                writableRoots,
                "writableRoots required"
        );

        this.allowedReadRoots =
                readableRoots.stream()
                        .map(this::resolveConfiguredRoot)
                        .toList();

        this.allowedWriteRoots =
                writableRoots.stream()
                        .map(this::resolveConfiguredRoot)
                        .toList();
    }

    @Override
    public String readFile(String relativePath) {

        Path path =
                resolveAllowed(
                        relativePath,
                        allowedReadRoots,
                        "read"
                );

        try {
            return Files.readString(path);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to read workspace file: "
                            + relativePath,
                    e
            );
        }
    }

    @Override
    public void writeFile(
            String relativePath,
            String content) {

        Objects.requireNonNull(
                content,
                "content required"
        );

        Path path =
                resolveAllowed(
                        relativePath,
                        allowedWriteRoots,
                        "write"
                );

        try {
            Path parent =
                    path.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(
                    path,
                    content
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to write workspace file: "
                            + relativePath,
                    e
            );
        }
    }

    @Override
    public boolean exists(String relativePath) {

        Path path =
                resolveAllowed(
                        relativePath,
                        allowedReadRoots,
                        "read"
                );

        return Files.exists(path);
    }

    private Path resolveConfiguredRoot(
            String relativeRoot) {

        if (relativeRoot == null
                || relativeRoot.isBlank()) {

            throw new IllegalArgumentException(
                    "configured root required"
            );
        }

        return projectRoot
                .resolve(relativeRoot)
                .toAbsolutePath()
                .normalize();
    }

    private Path resolveAllowed(
            String relativePath,
            List<Path> allowedRoots,
            String operation) {

        if (relativePath == null
                || relativePath.isBlank()) {

            throw new IllegalArgumentException(
                    "relativePath required"
            );
        }

        Path resolved =
                projectRoot
                        .resolve(relativePath)
                        .toAbsolutePath()
                        .normalize();

        boolean allowed =
                allowedRoots.stream()
                        .anyMatch(
                                resolved::startsWith
                        );

        if (!allowed) {
            throw new SecurityException(
                    "Agent workspace "
                            + operation
                            + " access denied: "
                            + relativePath
            );
        }

        return resolved;
    }
}