package com.cs.urlshortenerorchestrator.analytics.repository;

import com.cs.urlshortenerorchestrator.analytics.model.AnalyticsEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsEventRepositoryTest {

    private AnalyticsEventRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AnalyticsEventRepository();
        repository.clear(); // Ensure a clean state for each test
    }

    @Test
    void testSaveAndFindAll() {
        AnalyticsEvent event1 = new AnalyticsEvent("shortUrl1", Instant.now(), "ip1", "ua1");
        AnalyticsEvent event2 = new AnalyticsEvent("shortUrl2", Instant.now(), "ip2", "ua2");

        repository.save(event1);
        repository.save(event2);

        List<AnalyticsEvent> allEvents = repository.findAll();
        assertThat(allEvents).hasSize(2);
        assertThat(allEvents).containsExactlyInAnyOrder(event1, event2);
    }

    @Test
    void testFindByShortUrl() {
        Instant now = Instant.now();
        AnalyticsEvent event1 = new AnalyticsEvent("shortUrlA", now, "ip1", "ua1");
        AnalyticsEvent event2 = new AnalyticsEvent("shortUrlB", now, "ip2", "ua2");
        AnalyticsEvent event3 = new AnalyticsEvent("shortUrlA", now.plusSeconds(10), "ip3", "ua3");

        repository.save(event1);
        repository.save(event2);
        repository.save(event3);

        List<AnalyticsEvent> eventsForA = repository.findByShortUrl("shortUrlA");
        assertThat(eventsForA).hasSize(2);
        assertThat(eventsForA).containsExactlyInAnyOrder(event1, event3);

        List<AnalyticsEvent> eventsForC = repository.findByShortUrl("shortUrlC");
        assertThat(eventsForC).isEmpty();
    }

    @Test
    void testFindByShortUrlAndTimestampBetween() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        AnalyticsEvent event1 = new AnalyticsEvent("urlX", now.minusSeconds(60), "ip1", "ua1");
        AnalyticsEvent event2 = new AnalyticsEvent("urlX", now.minusSeconds(30), "ip2", "ua2");
        AnalyticsEvent event3 = new AnalyticsEvent("urlY", now.minusSeconds(10), "ip3", "ua3");
        AnalyticsEvent event4 = new AnalyticsEvent("urlX", now.plusSeconds(10), "ip4", "ua4");
        AnalyticsEvent event5 = new AnalyticsEvent("urlX", now.plusSeconds(60), "ip5", "ua5");

        repository.save(event1);
        repository.save(event2);
        repository.save(event3);
        repository.save(event4);
        repository.save(event5);

        Instant start = now.minusSeconds(45);
        Instant end = now.plusSeconds(15);

        List<AnalyticsEvent> filteredEvents = repository.findByShortUrlAndTimestampBetween("urlX", start, end);
        assertThat(filteredEvents).hasSize(2);
        assertThat(filteredEvents).containsExactlyInAnyOrder(event2, event4);

        // Test with no events in range
        List<AnalyticsEvent> noEvents = repository.findByShortUrlAndTimestampBetween("urlX", now.plusSeconds(100), now.plusSeconds(200));
        assertThat(noEvents).isEmpty();

        // Test with different shortUrl
        List<AnalyticsEvent> eventsForY = repository.findByShortUrlAndTimestampBetween("urlY", start, end);
        assertThat(eventsForY).hasSize(1);
        assertThat(eventsForY).containsExactly(event3);
    }

    @Test
    void testClear() {
        AnalyticsEvent event1 = new AnalyticsEvent("shortUrl1", Instant.now(), "ip1", "ua1");
        repository.save(event1);
        assertThat(repository.findAll()).hasSize(1);

        repository.clear();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void testFindAllReturnsImmutableList() {
        repository.save(new AnalyticsEvent("url", Instant.now(), "ip", "ua"));
        List<AnalyticsEvent> allEvents = repository.findAll();
        assertThat(allEvents).hasSize(1);
        // Attempting to modify the returned list should throw an UnsupportedOperationException
        assertThat(allEvents).isInstanceOf(List.class);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> allEvents.add(new AnalyticsEvent()));
    }
}
