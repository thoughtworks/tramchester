package com.tramchester.domain;

import com.tramchester.graph.TransportDataForTest;
import org.joda.time.LocalDate;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class StationsOpenTodayTest {

    @Test
    public void shouldTestSelectCorrectStations() {

        TransportDataForTest transportDataForTest = new TransportDataForTest();

        StationsOpenToday openToday = new StationsOpenToday(transportDataForTest);

        Set<Location> stations = openToday.getStations(new LocalDate(2016,6,20));
        assertEquals(5, stations.size());

        // same day, cached
        openToday.getStations(new LocalDate(2016,6,20));
        assertEquals(5, stations.size());

        // different day
        openToday.getStations(new LocalDate(2016,6,21));
        assertEquals(5, stations.size());

        stations = openToday.getStations(new LocalDate(2013,6,20));
        assertEquals(0, stations.size());
    }
}
