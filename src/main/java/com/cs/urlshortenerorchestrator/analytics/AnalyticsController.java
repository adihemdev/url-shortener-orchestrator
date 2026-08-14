package com.cs.urlshortenerorchestrator.analytics;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final EventStore eventStore;

    public AnalyticsController(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @PostMapping("/event")
    public ResponseEntity<String> recordEvent(@RequestBody EventRequest eventRequest) {
        if (eventRequest == null || eventRequest.shortUrl() == null || eventRequest.shortUrl().isBlank()) {
            return ResponseEntity.badRequest().body("Short URL is required");
        }
        Event event = new Event(
                eventRequest.shortUrl(),
                Instant.now(),
                eventRequest.ipAddress(),
                eventRequest.userAgent()
        );
        eventStore.storeEvent(event);
        return ResponseEntity.ok("Event recorded successfully");
    }

    @GetMapping("/events")
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventStore.getAllEvents());
    }

    @GetMapping("/events/{shortUrl}")
    public ResponseEntity<List<Event>> getEventsForShortUrl(@PathVariable String shortUrl) {
        if (shortUrl == null || shortUrl.isBlank()) {
            return ResponseEntity.badRequest().body(List.of());
        }
        return ResponseEntity.ok(eventStore.getEventsForShortUrl(shortUrl));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> getSummary() {
        Map<String, Long> summary = eventStore.getAllEvents().stream()
                .collect(Collectors.groupingBy(Event::getShortUrl, Collectors.counting()));
        return ResponseEntity.ok(summary);
    }

    // DTO for incoming event requests
    public record EventRequest(String shortUrl, String ipAddress, String userAgent) {}
}
