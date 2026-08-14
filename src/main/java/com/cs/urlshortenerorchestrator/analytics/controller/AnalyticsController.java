package com.cs.urlshortenerorchestrator.analytics.controller;

import com.cs.urlshortenerorchestrator.analytics.model.AnalyticsEvent;
import com.cs.urlshortenerorchestrator.analytics.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/event")
    public ResponseEntity<Void> recordAnalyticsEvent(@RequestBody AnalyticsEvent event) {
        // Ensure timestamp is set if not provided, or normalize it
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        analyticsService.recordEvent(event);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @GetMapping("/clicks/{shortUrl}")
    public ResponseEntity<Long> getTotalClicks(@PathVariable String shortUrl) {
        long clicks = analyticsService.getTotalClicks(shortUrl);
        return ResponseEntity.ok(clicks);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Long>> getDashboardData() {
        Map<String, Long> clicksByShortUrl = analyticsService.getClicksByShortUrl();
        return ResponseEntity.ok(clicksByShortUrl);
    }

    @GetMapping("/clicks-over-time/{shortUrl}")
    public ResponseEntity<Map<Instant, Long>> getClicksOverTime(
            @PathVariable String shortUrl,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        Map<Instant, Long> clicksOverTime = analyticsService.getClicksOverTime(shortUrl, start, end);
        return ResponseEntity.ok(clicksOverTime);
    }

    @GetMapping("/spike-detection/{shortUrl}")
    public ResponseEntity<Boolean> detectSpike(
            @PathVariable String shortUrl,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant periodEnd,
            @RequestParam long threshold) {
        boolean spikeDetected = analyticsService.detectSpike(shortUrl, periodStart, periodEnd, threshold);
        return ResponseEntity.ok(spikeDetected);
    }

    @GetMapping("/events")
    public ResponseEntity<List<AnalyticsEvent>> getAllEvents() {
        List<AnalyticsEvent> events = analyticsService.getAllEvents();
        return ResponseEntity.ok(events);
    }
}
