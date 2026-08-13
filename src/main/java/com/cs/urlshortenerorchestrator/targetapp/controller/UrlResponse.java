package com.cs.urlshortenerorchestrator.targetapp.controller;

public record UrlResponse(
        String shortCode,
        String longUrl,
        String createdAt) {}
