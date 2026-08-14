package com.cs.urlshortenerorchestrator.targetapp.service;

import com.cs.urlshortenerorchestrator.targetapp.config.ShortenerProperties;
import com.cs.urlshortenerorchestrator.targetapp.model.UrlMapping;
import com.cs.urlshortenerorchestrator.targetapp.repository.UrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Slf4j
@Service
public class UrlService {
    private static final SecureRandom random = new SecureRandom();

    private final UrlRepository urlRepository;
    private final ShortenerProperties properties;

    public UrlService(UrlRepository urlRepository, ShortenerProperties properties) {
        this.urlRepository = urlRepository;
        this.properties = properties;
    }

    /**
     * Shortens a long URL by generating a unique short code with collision handling.
     * On each iteration, generates a random short code and attempts to persist.
     * If a collision occurs (DataIntegrityViolationException on unique constraint),
     * retries up to maxRetries times before failing.
     *
     * @param longUrl the URL to shorten
     * @return UrlMapping containing the generated short code and original URL
     * @throws ShortCodeGenerationException if unable to generate unique code after max retries
     */
    public UrlMapping shortenUrl(String longUrl) {
        DataIntegrityViolationException lastException = null;

        for (int attempt = 0; attempt < properties.maxRetries(); attempt++) {
            try {
                String shortCode = generateShortCode();
                log.debug("Attempt {} to generate short code", attempt + 1);

                UrlMapping mapping = new UrlMapping(longUrl, shortCode);
                return urlRepository.save(mapping);
            } catch (DataIntegrityViolationException e) {
                lastException = e;
                log.debug("Short code collision detected on attempt {}", attempt + 1);
                if (attempt == properties.maxRetries() - 1) {
                    log.error("Failed to generate unique short code after {} attempts", properties.maxRetries());
                    throw new ShortCodeGenerationException(
                        "Failed to generate unique short code after " + properties.maxRetries() + " attempts",
                        lastException
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

    @Transactional
    public boolean deleteUrl(String shortCode) {
        if (urlRepository.existsByShortCode(shortCode)) {
            urlRepository.deleteByShortCode(shortCode);
            return true;
        }
        return false;
    }
}
