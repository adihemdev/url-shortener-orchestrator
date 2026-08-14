package com.cs.urlshortenerorchestrator.engine.agent;

public interface WorkspaceTool {

    String readFile(String relativePath);

    void writeFile(String relativePath, String content);

    boolean exists(String relativePath);
}