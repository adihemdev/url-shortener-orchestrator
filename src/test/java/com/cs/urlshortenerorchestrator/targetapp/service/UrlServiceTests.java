package com.cs.urlshortenerorchestrator.targetapp.service;

import com.cs.urlshortenerorchestrator.targetapp.config.ShortenerProperties;
import com.cs.urlshortenerorchestrator.targetapp.model.UrlMapping;
import com.cs.urlshortenerorchestrator.targetapp.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlService Unit Tests")
class UrlServiceTests {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private ShortenerProperties properties;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        urlService = new UrlService(urlRepository, properties);
    }

    private void configureShortenerProperties() {
        when(properties.codeLength()).thenReturn(6);
        when(properties.alphabet())
                .thenReturn("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
        when(properties.maxRetries()).thenReturn(10);
    }

    @Test
    @DisplayName("Should successfully generate and save short code on first attempt")
    void shouldGenerateShortCodeOnFirstAttempt() {

        configureShortenerProperties();

        String longUrl = "https://example.com/very/long/url";
        UrlMapping savedMapping = new UrlMapping(longUrl, "ABC123");

        when(urlRepository.save(any(UrlMapping.class))).thenReturn(savedMapping);

        UrlMapping result = urlService.shortenUrl(longUrl);

        assertNotNull(result);
        assertEquals(longUrl, result.getLongUrl());
        assertEquals(6, result.getShortCode().length());
        verify(urlRepository).save(any(UrlMapping.class));
    }

    @Test
    @DisplayName("Should retry on collision and succeed on second attempt")
    void shouldRetryOnCollisionAndSucceedEventually() {

        configureShortenerProperties();

        String longUrl = "https://example.com/collision/test";
        UrlMapping savedMapping = new UrlMapping(longUrl, "XYZ789");

        // Simulate collision on first attempt, success on second
        when(urlRepository.save(any(UrlMapping.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"))
                .thenReturn(savedMapping);

        UrlMapping result = urlService.shortenUrl(longUrl);

        assertNotNull(result);
        assertEquals(longUrl, result.getLongUrl());
        verify(urlRepository, times(2)).save(any(UrlMapping.class));
    }

    @Test
    @DisplayName("Should return long URL when short code exists")
    void shouldReturnLongUrlWhenShortCodeExists() {
        String shortCode = "ABC123";
        String longUrl = "https://example.com/target";

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(new UrlMapping(longUrl, shortCode)));

        Optional<String> result = urlService.getLongUrl(shortCode);

        assertTrue(result.isPresent());
        assertEquals(longUrl, result.get());
        verify(urlRepository).findByShortCode(shortCode);
    }

    @Test
    @DisplayName("Should return empty when short code does not exist")
    void shouldReturnEmptyWhenShortCodeDoesNotExist() {
        String shortCode = "NONEXIST";

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        Optional<String> result = urlService.getLongUrl(shortCode);

        assertTrue(result.isEmpty());
        verify(urlRepository).findByShortCode(shortCode);
    }

    @Test
    @DisplayName("Should throw exception when all short code generation attempts fail")
    void shouldThrowExceptionWhenRetriesExhausted() {

        configureShortenerProperties();

        String longUrl = "https://example.com/retry-exhaustion";

        when(urlRepository.save(any(UrlMapping.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        ShortCodeGenerationException exception = assertThrows(
                ShortCodeGenerationException.class,
                () -> urlService.shortenUrl(longUrl)
        );

        assertEquals(
                "Failed to generate unique short code after 10 attempts",
                exception.getMessage()
        );

        verify(urlRepository, times(10)).save(any(UrlMapping.class));
    }

    @Test
    @DisplayName("Deleting an existing short code should return true")
    void shouldReturnTrueWhenDeletingExistingShortCode() {
        String shortCode = "ABC123";
        when(urlRepository.existsByShortCode(shortCode)).thenReturn(true);
        doNothing().when(urlRepository).deleteByShortCode(shortCode);

        boolean result = urlService.deleteUrl(shortCode);

        assertTrue(result);
        verify(urlRepository).existsByShortCode(shortCode);
        verify(urlRepository).deleteByShortCode(shortCode);
    }

    @Test
    @DisplayName("Deleting a missing short code should return false")
    void shouldReturnFalseWhenDeletingMissingShortCode() {
        String shortCode = "NONEXIST";
        when(urlRepository.existsByShortCode(shortCode)).thenReturn(false);

        boolean result = urlService.deleteUrl(shortCode);

        assertFalse(result);
        verify(urlRepository).existsByShortCode(shortCode);
        verify(urlRepository, never()).deleteByShortCode(shortCode);
    }
}