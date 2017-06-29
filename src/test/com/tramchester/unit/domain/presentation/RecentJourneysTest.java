package com.tramchester.unit.domain.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.Sets;
import com.tramchester.domain.RecentJourneys;
import com.tramchester.domain.Timestamped;
import org.joda.time.DateTime;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

public class RecentJourneysTest {

    @Test
    public void shouldGetAndSetValues() {
        RecentJourneys recentJourneys = new RecentJourneys();
        Timestamped timestamped = new Timestamped("id", DateTime.now());
        HashSet<Timestamped> set = Sets.newHashSet(timestamped);
        recentJourneys.setRecentIds(set);

        assertEquals(set, recentJourneys.getRecentIds());
    }

    @Test
    public void shouldEncodeAndDecodeCookieString() throws IOException {
        RecentJourneys recentJourneys = new RecentJourneys();
        Timestamped timestamped = new Timestamped("id", DateTime.now());
        HashSet<Timestamped> set = Sets.newHashSet(timestamped);
        recentJourneys.setRecentIds(set);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        String cookieText = RecentJourneys.encodeCookie(mapper,recentJourneys);

        RecentJourneys result = RecentJourneys.decodeCookie(mapper, cookieText);
        assertEquals(recentJourneys, result);
    }
}
