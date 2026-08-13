package com.cs.urlshortenerorchestrator.targetapp.controller;

public record ErrorResponse(
        String message,
        String timestamp,
        int status
) {}