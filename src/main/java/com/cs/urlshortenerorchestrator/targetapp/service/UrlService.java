package com.cs.urlshortenerorchestrator.targetapp.service;

import com.cs.urlshortenerorchestrator.targetapp.config.ShortenerProperties;
import com.cs.urlshortenerorchestrator.targetapp.model.UrlMapping;
import com.cs.urlshortenerorchestrator.targetapp.repository.UrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
public class UrlService {
    private static final Random random = new Random();

    private final UrlRepository urlRepository;
    private final ShortenerProperties properties;

    public UrlService(UrlRepository urlRepository, ShortenerProperties properties) {
        this.urlRepository = urlRepository;
        this.properties = properties;
    }

    @Transactional
    public UrlMapping shortenUrl(String longUrl) {
        for (int attempt = 0; attempt < properties.maxRetries(); attempt++) {
            try {
                String shortCode = generateShortCode();
                log.debug("Attempt {} to generate short code", attempt + 1);

                UrlMapping mapping = new UrlMapping(longUrl, shortCode);
                return urlRepository.save(mapping);
            } catch (DataIntegrityViolationException e) {
                log.debug("Short code collision detected on attempt {}", attempt + 1);
                if (attempt == properties.maxRetries() - 1) {
                    log.error("Failed to generate unique short code after {} attempts", properties.maxRetries());
                    throw new ShortCodeGenerationException(
                        "Failed to generate unique short code after " + properties.maxRetries() + " attempts"
                    );
                }
            }
        }
        throw new ShortCodeGenerationException("Unexpected error in shortenUrl");
    }

    private String generateShortCode() {
        String alphabet = properties.alphabet();
        StringBuilder shortCode = new StringBuilder();
        for (int i = 0; i < properties.codeLength(); i++) {
            shortCode.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return shortCode.toString();
    }

    @Transactional(readOnly = true)
    public Optional<String> getLongUrl(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .map(UrlMapping::getLongUrl);
    }
}