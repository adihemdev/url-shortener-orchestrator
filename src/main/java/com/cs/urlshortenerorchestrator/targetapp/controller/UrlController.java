package com.cs.urlshortenerorchestrator.targetapp.controller;

import com.cs.urlshortenerorchestrator.targetapp.model.UrlMapping;
import com.cs.urlshortenerorchestrator.targetapp.service.UrlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/urls")
@Validated
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping
    public ResponseEntity<UrlResponse> shortenUrl(@RequestBody @Validated UrlRequest request) {
        log.debug("Received request to shorten URL");
        UrlMapping mapping = urlService.shortenUrl(request.longUrl());
        log.info("Successfully created short code: {}", mapping.getShortCode());
        UrlResponse response = new UrlResponse(mapping.getShortCode(), mapping.getLongUrl(),
                mapping.getCreatedAt().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectToLongUrl(@PathVariable String shortCode) {
        log.debug("Redirect request for short code: {}", shortCode);
        Optional<String> longUrl = urlService.getLongUrl(shortCode);
        return longUrl.<ResponseEntity<Void>>map(s -> {
            log.debug("Redirecting {}", shortCode);
            return ResponseEntity.status(301).location(URI.create(s)).build();
        }).orElseGet(() -> {
            log.warn("Short code not found: {}", shortCode);
            return ResponseEntity.notFound().build();
        });
    }

}