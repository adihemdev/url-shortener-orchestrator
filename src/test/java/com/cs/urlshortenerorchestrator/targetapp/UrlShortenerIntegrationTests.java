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
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Transactional
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

    @Test
    @DisplayName("DELETE /api/v1/urls/{shortCode} should delete URL mapping and return 204")
    void shouldDeleteUrlMappingSuccessfully() {

        String longUrl =
                "https://www.example.com/url/to/delete";

        HttpEntity<UrlRequest> createRequest =
                new HttpEntity<>(
                        new UrlRequest(longUrl),
                        jsonHeaders()
                );

        ResponseEntity<UrlResponse> createResponse =
                restTemplate.exchange(
                        "/api/v1/urls",
                        HttpMethod.POST,
                        createRequest,
                        UrlResponse.class
                );

        assertEquals(
                HttpStatus.OK,
                createResponse.getStatusCode()
        );

        assertNotNull(
                createResponse.getBody()
        );

        String shortCode =
                createResponse.getBody()
                        .shortCode();

        ResponseEntity<Void> deleteResponse =
                restTemplate.exchange(
                        "/api/v1/urls/" + shortCode,
                        HttpMethod.DELETE,
                        null,
                        Void.class
                );

        assertEquals(
                HttpStatus.NO_CONTENT,
                deleteResponse.getStatusCode()
        );

        Optional<UrlMapping> deleted =
                urlRepository.findByShortCode(
                        shortCode
                );

        assertFalse(
                deleted.isPresent()
        );
    }

    @Test
    @DisplayName("DELETE /api/v1/urls/{shortCode} should return 404 for missing short code")
    void shouldReturn404ForMissingShortCodeOnDelete() {
        String nonExistentShortCode = "NONEXIST";

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/v1/urls/" + nonExistentShortCode,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertEquals(HttpStatus.NOT_FOUND, deleteResponse.getStatusCode());
    }
}
