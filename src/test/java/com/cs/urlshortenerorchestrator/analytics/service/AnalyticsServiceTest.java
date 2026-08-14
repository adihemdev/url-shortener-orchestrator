package com.cs.urlshortenerorchestrator.analytics.service;

import com.cs.urlshortenerorchestrator.analytics.model.AnalyticsEvent;
import com.cs.urlshortenerorchestrator.analytics.repository.AnalyticsEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AnalyticsServiceTest {

    @Mock
    private AnalyticsEventRepository repository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRecordEvent() {
        AnalyticsEvent event = new AnalyticsEvent("shortUrl1", Instant.now(), "ip1", "ua1");
        analyticsService.recordEvent(event);
        verify(repository, times(1)).save(event);
    }

    @Test
    void testGetTotalClicks() {
        String shortUrl = "shortUrlA";
        List<AnalyticsEvent> events = Arrays.asList(
                new AnalyticsEvent(shortUrl, Instant.now(), "ip1", "ua1"),
                new AnalyticsEvent(shortUrl, Instant.now(), "ip2", "ua2")
        );
        when(repository.findByShortUrl(shortUrl)).thenReturn(events);

        long totalClicks = analyticsService.getTotalClicks(shortUrl);
        assertThat(totalClicks).isEqualTo(2);
        verify(repository, times(1)).findByShortUrl(shortUrl);
    }

    @Test
    void testGetTotalClicksForNonExistentUrl() {
        String shortUrl = "nonExistentUrl";
        when(repository.findByShortUrl(shortUrl)).thenReturn(Collections.emptyList());

        long totalClicks = analyticsService.getTotalClicks(shortUrl);
        assertThat(totalClicks).isEqualTo(0);
        verify(repository, times(1)).findByShortUrl(shortUrl);
    }

    @Test
    void testGetClicksByShortUrl() {
        Instant now = Instant.now();
        List<AnalyticsEvent> allEvents = Arrays.asList(
                new AnalyticsEvent("url1", now, "ip1", "ua1"),
                new AnalyticsEvent("url2", now, "ip2", "ua2"),
                new AnalyticsEvent("url1", now, "ip3", "ua3"),
                new AnalyticsEvent("url3", now, "ip4", "ua4")
        );
        when(repository.findAll()).thenReturn(allEvents);

        Map<String, Long> clicksByShortUrl = analyticsService.getClicksByShortUrl();
        assertThat(clicksByShortUrl).hasSize(3);
        assertThat(clicksByShortUrl).containsEntry("url1", 2L);
        assertThat(clicksByShortUrl).containsEntry("url2", 1L);
        assertThat(clicksByShortUrl).containsEntry("url3", 1L);
        verify(repository, times(1)).findAll();
    }

    @Test
    void testGetClicksByShortUrlWhenNoEvents() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        Map<String, Long> clicksByShortUrl = analyticsService.getClicksByShortUrl();
        assertThat(clicksByShortUrl).isEmpty();
        verify(repository, times(1)).findAll();
    }

    @Test
    void testGetClicksOverTime() {
        String shortUrl = "urlX";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

        Instant time1 = start.plusSeconds(10);
        Instant time2 = start.plusSeconds(20);
        Instant time3 = start.plusSeconds(10);

        List<AnalyticsEvent> eventsInPeriod = Arrays.asList(
                new AnalyticsEvent(shortUrl, time1, "ip1", "ua1"),
                new AnalyticsEvent(shortUrl, time2, "ip2", "ua2"),
                new AnalyticsEvent(shortUrl, time3, "ip3", "ua3")
        );
        when(repository.findByShortUrlAndTimestampBetween(shortUrl, start, end)).thenReturn(eventsInPeriod);

        Map<Instant, Long> clicksOverTime = analyticsService.getClicksOverTime(shortUrl, start, end);
        assertThat(clicksOverTime).hasSize(2);
        assertThat(clicksOverTime).containsEntry(time1, 2L);
        assertThat(clicksOverTime).containsEntry(time2, 1L);
        verify(repository, times(1)).findByShortUrlAndTimestampBetween(shortUrl, start, end);
    }

    @Test
    void testGetClicksOverTimeWhenNoEventsInPeriod() {
        String shortUrl = "urlX";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

        when(repository.findByShortUrlAndTimestampBetween(shortUrl, start, end)).thenReturn(Collections.emptyList());

        Map<Instant, Long> clicksOverTime = analyticsService.getClicksOverTime(shortUrl, start, end);
        assertThat(clicksOverTime).isEmpty();
        verify(repository, times(1)).findByShortUrlAndTimestampBetween(shortUrl, start, end);
    }

    @Test
    void testDetectSpike_spikeDetected() {
        String shortUrl = "urlS";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();
        long threshold = 2;

        List<AnalyticsEvent> events = Arrays.asList(
                new AnalyticsEvent(shortUrl, start.plusSeconds(10), "ip1", "ua1"),
                new AnalyticsEvent(shortUrl, start.plusSeconds(20), "ip2", "ua2"),
                new AnalyticsEvent(shortUrl, start.plusSeconds(30), "ip3", "ua3")
        );
        when(repository.findByShortUrlAndTimestampBetween(shortUrl, start, end)).thenReturn(events);

        boolean spikeDetected = analyticsService.detectSpike(shortUrl, start, end, threshold);
        assertThat(spikeDetected).isTrue();
        verify(repository, times(1)).findByShortUrlAndTimestampBetween(shortUrl, start, end);
    }

    @Test
    void testDetectSpike_noSpike() {
        String shortUrl = "urlS";
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now();
        long threshold = 5;

        List<AnalyticsEvent> events = Arrays.asList(
                new AnalyticsEvent(shortUrl, start.plusSeconds(10), "ip1", "ua1"),
                new AnalyticsEvent(shortUrl, start.plusSeconds(20), "ip2", "ua2")
        );
        when(repository.findByShortUrlAndTimestampBetween(shortUrl, start, end)).thenReturn(events);

        boolean spikeDetected = analyticsService.detectSpike(shortUrl, start, end, threshold);
        assertThat(spikeDetected).isFalse();
        verify(repository, times(1)).findByShortUrlAndTimestampBetween(shortUrl, start, end);
    }

    @Test
    void testGetAllEvents() {
        List<AnalyticsEvent> expectedEvents = Arrays.asList(
                new AnalyticsEvent("url1", Instant.now(), "ip1", "ua1"),
                new AnalyticsEvent("url2", Instant.now(), "ip2", "ua2")
        );
        when(repository.findAll()).thenReturn(expectedEvents);

        List<AnalyticsEvent> actualEvents = analyticsService.getAllEvents();
        assertThat(actualEvents).isEqualTo(expectedEvents);
        verify(repository, times(1)).findAll();
    }

    @Test
    void testGetAllEventsWhenNoneExist() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<AnalyticsEvent> actualEvents = analyticsService.getAllEvents();
        assertThat(actualEvents).isEmpty();
        verify(repository, times(1)).findAll();
    }
}
