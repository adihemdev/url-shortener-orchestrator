package com.cs.urlshortenerorchestrator.targetapp;

import com.cs.urlshortenerorchestrator.targetapp.controller.UrlRequest;
import com.cs.urlshortenerorchestrator.targetapp.controller.UrlResponse;
import com.cs.urlshortenerorchestrator.targetapp.model.UrlMapping;
import com.cs.urlshortenerorchestrator.targetapp.repository.UrlRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DisplayName("URL Shortener Integration Tests")
class UrlShortenerIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UrlRepository urlRepository;

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    @DisplayName("POST /api/v1/urls should persist URL mapping to database")
    void shouldPersistUrlMappingToDatabase() {
        String longUrl = "https://www.example.com/very/long/url/path";

        HttpEntity<UrlRequest> request = new HttpEntity<>(
                new UrlRequest(longUrl),
                jsonHeaders()
        );

        ResponseEntity<UrlResponse> response = restTemplate.exchange(
                "/api/v1/urls",
                HttpMethod.POST,
                request,
                UrlResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(longUrl, response.getBody().longUrl());
        assertNotNull(response.getBody().shortCode());

        Optional<UrlMapping> saved =
                urlRepository.findByShortCode(response.getBody().shortCode());

        assertTrue(saved.isPresent());
        assertEquals(longUrl, saved.get().getLongUrl());
    }


    @Test
    @DisplayName("POST /api/v1/urls should reject invalid URL format")
    void shouldRejectInvalidUrlFormat() {
        HttpEntity<UrlRequest> request = new HttpEntity<>(
                new UrlRequest("not-a-valid-url"),
                jsonHeaders()
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/urls",
                HttpMethod.POST,
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}