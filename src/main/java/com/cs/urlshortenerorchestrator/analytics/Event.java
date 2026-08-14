package com.cs.urlshortenerorchestrator.analytics;

import java.time.Instant;
import java.util.Objects;

public class Event {
    private final String shortUrl;
    private final Instant timestamp;
    private final String ipAddress;
    private final String userAgent;

    public Event(String shortUrl, Instant timestamp, String ipAddress, String userAgent) {
        this.shortUrl = Objects.requireNonNull(shortUrl, "Short URL cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.ipAddress = ipAddress; // Can be null
        this.userAgent = userAgent; // Can be null
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return shortUrl.equals(event.shortUrl) && timestamp.equals(event.timestamp) && Objects.equals(ipAddress, event.ipAddress) && Objects.equals(userAgent, event.userAgent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortUrl, timestamp, ipAddress, userAgent);
    }

    @Override
    public String toString() {
        return "Event{" +
               "shortUrl='" + shortUrl + '\'' +
               ", timestamp=" + timestamp +
               ", ipAddress='" + (ipAddress != null ? ipAddress : "N/A") + '\'' +
               ", userAgent='" + (userAgent != null ? userAgent : "N/A") + '\'' +
               '}';
    }
}
