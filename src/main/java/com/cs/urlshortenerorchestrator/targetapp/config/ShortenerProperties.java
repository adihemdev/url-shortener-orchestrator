package com.cs.urlshortenerorchestrator.targetapp.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.shortener")
public record ShortenerProperties(
        @Min(value = 4, message = "Code length must be at least 4 characters")
        @Max(value = 10, message = "Code length must not exceed 10 characters (DB constraint)")
        int codeLength,

        @NotBlank(message = "Alphabet cannot be blank")
        String alphabet,

        @Min(value = 1, message = "Max retries must be at least 1")
        int maxRetries
) {}