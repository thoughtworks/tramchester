package com.tramchester.unit.domain;

import com.tramchester.domain.LocationSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class LocationSetTest {

    private List<Station> stationList;
    private MyLocation location;

    @BeforeEach
    void beforeEachTestRuns() {
        stationList = Arrays.asList(of(Altrincham), of(Bury), of(Cornbrook));
        location = new MyLocation(TestEnv.nearShudehill);
    }

    private void assertListElementsPresent(LocationSet locationSet) {
        assertEquals(stationList.size(), locationSet.size());

        assertTrue(locationSet.contains(of(Altrincham)));
        assertTrue(locationSet.contains(of(Bury)));
        assertTrue(locationSet.contains(of(Cornbrook)));
    }

    @Test
    void shouldCreateSingleton() {
        LocationSet locationSet = LocationSet.singleton(of(Altrincham));

        assertEquals(1, locationSet.size());
        assertTrue(locationSet.contains(of(Altrincham)));
        assertFalse(locationSet.contains(of(StPetersSquare)));
    }

    @Test
    void shouldCreateFromSet() {
        Set<Station> stations = new HashSet<>(stationList);

        LocationSet locationSet = new LocationSet(stations);

        assertListElementsPresent(locationSet);
        assertFalse(locationSet.contains(of(StPetersSquare)));
    }

    @Test
    void shouldCollectStationsAsExpected() {

        Stream<Station> stream = stationList.stream();

        LocationSet locationSet = stream.collect(LocationSet.stationCollector());

        assertListElementsPresent(locationSet);
        assertFalse(locationSet.contains(of(StPetersSquare)));
    }

    @Test
    void shouldHaveAdd() {

        LocationSet locationSet = new LocationSet();

        assertTrue(locationSet.isEmpty());

        locationSet.add(of(Altrincham));
        locationSet.add(of(Altrincham));
        locationSet.add(of(Bury));
        locationSet.add(of(Cornbrook));

        assertListElementsPresent(locationSet);

        locationSet.add(location);

        assertEquals(4, locationSet.size());
        assertTrue(locationSet.contains(location));

    }

    @Test
    void shouldGetMixedStream() {

        LocationSet locationSet = new LocationSet(new HashSet<>(stationList));

        locationSet.add(location);

        assertEquals(4, locationSet.size());

        Set<Location<?>> result = locationSet.stream().collect(Collectors.toSet());

        assertEquals(4, result.size());

        assertTrue(result.contains(location));
        assertTrue(result.contains(of(Altrincham)));
        assertTrue(result.contains(of(Bury)));
        assertTrue(result.contains(of(Cornbrook)));
    }

    @Test
    void shouldGetStationOnlyStream() {

        LocationSet locationSet = new LocationSet(new HashSet<>(stationList));

        locationSet.add(location);

        assertEquals(4, locationSet.size());

        Set<Station> result = locationSet.stationsOnlyStream().collect(Collectors.toSet());

        assertEquals(3, result.size());

        assertTrue(result.contains(of(Altrincham)));
        assertTrue(result.contains(of(Bury)));
        assertTrue(result.contains(of(Cornbrook)));
    }
}
