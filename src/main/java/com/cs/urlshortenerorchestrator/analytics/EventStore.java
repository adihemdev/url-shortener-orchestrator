package com.cs.urlshortenerorchestrator.analytics;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Component
public class EventStore {
    private final List<Event> events = new CopyOnWriteArrayList<>();

    public void storeEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        this.events.add(event);
    }

    public List<Event> getAllEvents() {
        return List.copyOf(events);
    }

    public List<Event> getEventsForShortUrl(String shortUrl) {
        if (shortUrl == null || shortUrl.isBlank()) {
            throw new IllegalArgumentException("Short URL cannot be null or blank");
        }
        return events.stream()
                .filter(event -> event.getShortUrl().equals(shortUrl))
                .collect(Collectors.toUnmodifiableList());
    }

    public void clear() {
        this.events.clear();
    }
}
