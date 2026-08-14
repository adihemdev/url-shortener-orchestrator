package com.cs.urlshortenerorchestrator.analytics.model;

import java.time.Instant;
import java.util.Objects;

public class AnalyticsEvent {
    private String shortUrl;
    private Instant timestamp;
    private String ipAddress;
    private String userAgent;

    public AnalyticsEvent() {
    }

    public AnalyticsEvent(String shortUrl, Instant timestamp, String ipAddress, String userAgent) {
        this.shortUrl = shortUrl;
        this.timestamp = timestamp;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalyticsEvent that = (AnalyticsEvent) o;
        return Objects.equals(shortUrl, that.shortUrl) && Objects.equals(timestamp, that.timestamp) && Objects.equals(ipAddress, that.ipAddress) && Objects.equals(userAgent, that.userAgent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortUrl, timestamp, ipAddress, userAgent);
    }

    @Override
    public String toString() {
        return "AnalyticsEvent{" +
               "shortUrl='" + shortUrl + '\'' +
               ", timestamp=" + timestamp +
               ", ipAddress='" + ipAddress + '\'' +
               ", userAgent='" + userAgent + '\'' +
               '}';
    }
}
