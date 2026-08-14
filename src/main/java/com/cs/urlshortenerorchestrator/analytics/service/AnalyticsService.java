package com.cs.urlshortenerorchestrator.analytics.service;

import com.cs.urlshortenerorchestrator.analytics.model.AnalyticsEvent;
import com.cs.urlshortenerorchestrator.analytics.repository.AnalyticsEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final AnalyticsEventRepository repository;

    public AnalyticsService(AnalyticsEventRepository repository) {
        this.repository = repository;
    }

    public void recordEvent(AnalyticsEvent event) {
        repository.save(event);
    }

    public long getTotalClicks(String shortUrl) {
        return repository.findByShortUrl(shortUrl).size();
    }

    public Map<String, Long> getClicksByShortUrl() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(AnalyticsEvent::getShortUrl, Collectors.counting()));
    }

    public Map<Instant, Long> getClicksOverTime(String shortUrl, Instant start, Instant end) {
        return repository.findByShortUrlAndTimestampBetween(shortUrl, start, end).stream()
                .collect(Collectors.groupingBy(AnalyticsEvent::getTimestamp, Collectors.counting()));
    }

    public boolean detectSpike(String shortUrl, Instant periodStart, Instant periodEnd, long threshold) {
        long clicksInPeriod = repository.findByShortUrlAndTimestampBetween(shortUrl, periodStart, periodEnd).size();
        return clicksInPeriod > threshold;
    }

    public List<AnalyticsEvent> getAllEvents() {
        return repository.findAll();
    }
}
