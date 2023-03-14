package com.tramchester.unit.domain.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.presentation.Timestamped;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecentJourneysTest {

    private ObjectMapper mapper;
    private IdForDTO id;

    @BeforeEach
    void beforeEachTestRuns() {
        id = new IdForDTO("id");
        mapper = new ObjectMapper();
    }

    @Test
    void shouldGetAndSetValues() {
        RecentJourneys recentJourneys = new RecentJourneys();
        Timestamped timestamped = new Timestamped(id, TestEnv.LocalNow());
        HashSet<Timestamped> set = Sets.newHashSet(timestamped);
        recentJourneys.setRecentIds(set);

        assertEquals(set, recentJourneys.getRecentIds());
    }

    @Test
    void shouldRoundTripCookieString() throws IOException {
        RecentJourneys recentJourneys = new RecentJourneys();
        Timestamped timestamped = new Timestamped(id, TestEnv.LocalNow());
        HashSet<Timestamped> set = Sets.newHashSet(timestamped);
        recentJourneys.setRecentIds(set);

        String cookieText = RecentJourneys.encodeCookie(mapper,recentJourneys);

        RecentJourneys result = RecentJourneys.decodeCookie(mapper, cookieText);
        assertEquals(recentJourneys, result);
    }

    @Test
    void shouldDecodeExistingCookieFormat() throws IOException {
        String cookie = "%7B%22recentIds%22%3A%5B%7B%22when%22%3A1541456199236%2C%22id%22%3A%22id%22%7D%5D%7D";
        RecentJourneys result = RecentJourneys.decodeCookie(mapper, cookie);

        long longValue = 1541456199236L;
        LocalDateTime expected = Instant.ofEpochMilli(longValue).atZone(TramchesterConfig.TimeZoneId).toLocalDateTime();

        Set<Timestamped> recentIds = result.getRecentIds();
        assertEquals(1, recentIds.size());
        assertTrue(recentIds.contains(new Timestamped(id,expected)));
    }

    @Test
    void shouldEncodeExistingCookieFormat() throws IOException {
        RecentJourneys recents = new RecentJourneys();

        String exepected = "%7B%22recentIds%22%3A%5B%7B%22when%22%3A1541456199236%2C%22id%22%3A%22id%22%7D%5D%7D";

        LocalDateTime dateTime = LocalDateTime.parse("2018-11-05T22:16:39.236");
        recents.setTimestamps(Sets.newHashSet(Collections.singletonList(new Timestamped(id, dateTime))));

        String result = RecentJourneys.encodeCookie(mapper, recents);
        assertEquals(exepected,result);
    }
}
