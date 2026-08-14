package com.cs.urlshortenerorchestrator.analytics.repository;

import com.cs.urlshortenerorchestrator.analytics.model.AnalyticsEvent;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Repository
public class AnalyticsEventRepository {
    private final List<AnalyticsEvent> events = new CopyOnWriteArrayList<>();

    public void save(AnalyticsEvent event) {
        events.add(event);
    }

    public List<AnalyticsEvent> findAll() {
        return List.copyOf(events);
    }

    public List<AnalyticsEvent> findByShortUrl(String shortUrl) {
        return events.stream()
                .filter(event -> event.getShortUrl().equals(shortUrl))
                .collect(Collectors.toList());
    }

    public List<AnalyticsEvent> findByShortUrlAndTimestampBetween(String shortUrl, Instant start, Instant end) {
        return events.stream()
                .filter(event -> event.getShortUrl().equals(shortUrl))
                .filter(event -> !event.getTimestamp().isBefore(start) && !event.getTimestamp().isAfter(end))
                .collect(Collectors.toList());
    }

    public void clear() {
        events.clear();
    }
}
