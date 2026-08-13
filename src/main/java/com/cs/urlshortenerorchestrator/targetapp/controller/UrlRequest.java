package com.cs.urlshortenerorchestrator.targetapp.controller;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record UrlRequest (
    @NotBlank(message = "URL cannot be blank")
    @URL(message = "Invalid URL format")
    String longUrl
) {}
