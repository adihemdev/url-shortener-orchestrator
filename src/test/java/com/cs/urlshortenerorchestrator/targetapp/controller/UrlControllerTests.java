package com.cs.urlshortenerorchestrator.targetapp.controller;

import com.cs.urlshortenerorchestrator.targetapp.model.UrlMapping;
import com.cs.urlshortenerorchestrator.targetapp.service.UrlService;
import com.cs.urlshortenerorchestrator.targetapp.service.ShortCodeGenerationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
@DisplayName("UrlController Tests")
class UrlControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlService urlService;

    @Test
    @DisplayName("POST /api/v1/urls should shorten URL and return short code")
    void shouldShortenUrlSuccessfully() throws Exception {

        String longUrl = "https://www.example.com/very/long/url/path";
        UrlMapping mapping = new UrlMapping(longUrl, "ABC123");
        mapping.setCreatedAt(Instant.now());

        when(urlService.shortenUrl(longUrl)).thenReturn(mapping);


        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"longUrl\": \"" + longUrl + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("ABC123"))
                .andExpect(jsonPath("$.longUrl").value(longUrl));

        verify(urlService).shortenUrl(longUrl);
    }

    @Test
    @DisplayName("POST /api/v1/urls should validate URL format")
    void shouldRejectInvalidUrlFormat() throws Exception {

        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"longUrl\": \"not-a-valid-url\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/urls should reject empty URL")
    void shouldRejectEmptyUrl() throws Exception {

        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"longUrl\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/urls/{shortCode} should redirect with 301 status")
    void shouldRedirectWithPermanentStatus() throws Exception {

        String shortCode = "ABC123";
        String longUrl = "https://www.example.com/target";

        when(urlService.getLongUrl(shortCode)).thenReturn(Optional.of(longUrl));


        mockMvc.perform(get("/api/v1/urls/" + shortCode))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl(longUrl));

        verify(urlService).getLongUrl(shortCode);
    }

    @Test
    @DisplayName("GET /api/v1/urls/{shortCode} should return 404 when code not found")
    void shouldReturn404WhenShortCodeNotFound() throws Exception {

        String shortCode = "INVALID";

        when(urlService.getLongUrl(shortCode)).thenReturn(Optional.empty());


        mockMvc.perform(get("/api/v1/urls/" + shortCode))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/urls should handle service exceptions gracefully")
    void shouldHandleServiceExceptionsGracefully() throws Exception {
        String longUrl = "https://www.example.com/test";

        when(urlService.shortenUrl(longUrl))
                .thenThrow(new ShortCodeGenerationException("Failed to generate unique short code after 10 attempts"));

        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"longUrl\": \"" + longUrl + "\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    @DisplayName("DELETE /api/v1/urls/{shortCode} should return 204 when short code exists")
    void shouldReturn204WhenShortCodeExistsOnDelete() throws Exception {
        String shortCode = "ABC123";

        when(urlService.deleteUrl(shortCode)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/urls/" + shortCode))
                .andExpect(status().isNoContent());

        verify(urlService).deleteUrl(shortCode);
    }

    @Test
    @DisplayName("DELETE /api/v1/urls/{shortCode} should return 404 when short code does not exist")
    void shouldReturn404WhenShortCodeDoesNotExistOnDelete() throws Exception {
        String shortCode = "NONEXIST";

        when(urlService.deleteUrl(shortCode)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/urls/" + shortCode))
                .andExpect(status().isNotFound());

        verify(urlService).deleteUrl(shortCode);
    }
}