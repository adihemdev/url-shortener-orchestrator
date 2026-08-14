package com.cs.urlshortenerorchestrator.analytics.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsEventTest {

    @Test
    void testNoArgsConstructorAndSetters() {
        AnalyticsEvent event = new AnalyticsEvent();
        String shortUrl = "abc";
        Instant timestamp = Instant.now();
        String ipAddress = "192.168.1.1";
        String userAgent = "Mozilla/5.0";

        event.setShortUrl(shortUrl);
        event.setTimestamp(timestamp);
        event.setIpAddress(ipAddress);
        event.setUserAgent(userAgent);

        assertThat(event.getShortUrl()).isEqualTo(shortUrl);
        assertThat(event.getTimestamp()).isEqualTo(timestamp);
        assertThat(event.getIpAddress()).isEqualTo(ipAddress);
        assertThat(event.getUserAgent()).isEqualTo(userAgent);
    }

    @Test
    void testAllArgsConstructor() {
        String shortUrl = "xyz";
        Instant timestamp = Instant.now();
        String ipAddress = "10.0.0.1";
        String userAgent = "Chrome";

        AnalyticsEvent event = new AnalyticsEvent(shortUrl, timestamp, ipAddress, userAgent);

        assertThat(event.getShortUrl()).isEqualTo(shortUrl);
        assertThat(event.getTimestamp()).isEqualTo(timestamp);
        assertThat(event.getIpAddress()).isEqualTo(ipAddress);
        assertThat(event.getUserAgent()).isEqualTo(userAgent);
    }

    @Test
    void testEqualsAndHashCode() {
        Instant now = Instant.now();
        AnalyticsEvent event1 = new AnalyticsEvent("url1", now, "ip1", "ua1");
        AnalyticsEvent event2 = new AnalyticsEvent("url1", now, "ip1", "ua1");
        AnalyticsEvent event3 = new AnalyticsEvent("url2", now, "ip1", "ua1");

        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
        assertThat(event1).isNotEqualTo(event3);
        assertThat(event1.hashCode()).isNotEqualTo(event3.hashCode());

        assertThat(event1).isNotEqualTo(null);
        assertThat(event1).isNotEqualTo(new Object());
    }

    @Test
    void testToString() {
        Instant now = Instant.now();
        AnalyticsEvent event = new AnalyticsEvent("url1", now, "ip1", "ua1");
        String expectedToString = "AnalyticsEvent{shortUrl='url1', timestamp=" + now + ", ipAddress='ip1', userAgent='ua1'}";
        assertThat(event.toString()).isEqualTo(expectedToString);
    }
}
