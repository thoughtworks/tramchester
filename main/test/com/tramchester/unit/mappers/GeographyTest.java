package com.tramchester.unit.mappers;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.mappers.Geography;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tec.units.ri.quantity.Quantities;
import tec.units.ri.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Time;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.KnownLocations.nearAltrincham;
import static org.junit.jupiter.api.Assertions.*;
import static tec.units.ri.unit.Units.METRE;

public class GeographyTest {
    private Geography geography;
    private TramchesterConfig config;

    @BeforeEach
    void onceBeforeEachTest() {
        config = TestEnv.GET();
        geography = new Geography(config);
    }

    public static Quantity<Length> BetweenStPeterSqAndPiccGardens = Quantities.getQuantity(463.7D, METRE);


    @Test
    void shouldGetWalkingTime() {

        Location<?> start = TramStations.StPetersSquare.fake();
        Location<?> end = TramStations.PiccadillyGardens.fake();

        int expected = TestEnv.calcCostInMinutes(start, end, config.getWalkingMPH());

        Quantity<Time> result = geography.getWalkingTime(BetweenStPeterSqAndPiccGardens);

        assertEquals(expected, Math.ceil(result.to(Units.MINUTE).getValue().doubleValue()));
    }

    @Test
    void shouldGetWalkingTimeInMins() {

        Location<?> start = TramStations.StPetersSquare.fake();
        Location<?> end = TramStations.PiccadillyGardens.fake();

        int expectedSeconds = 345;

        Duration result = geography.getWalkingDuration(start, end);

        assertEquals(expectedSeconds, result.getSeconds());
    }

    @Test
    void shouldGetDistanceBetweenLocations() {
        Station stPeters = TramStations.StPetersSquare.fake();
        Station piccGardens = TramStations.PiccadillyGardens.fake();

        Quantity<Length> distance = geography.getDistanceBetweenInMeters(stPeters, piccGardens);

        assertEquals(BetweenStPeterSqAndPiccGardens.getValue().doubleValue(),
                distance.getValue().doubleValue(), 0.1);
    }

    @Test
    void shouldGetNearToLocationSorted() {
        Station stationA = TramStations.Altrincham.fake();
        Station stationB = TramStations.PiccadillyGardens.fake();
        Station stationC = TramStations.Shudehill.fake();

        MyLocation myLocation = new MyLocation(nearAltrincham.latLong());

        Geography.LocationsSource<Station> provider = () -> Stream.of(stationC, stationA, stationB);

        List<Station> results = geography.
                getNearToSorted(provider, myLocation.getGridPosition(), MarginInMeters.of(20000)).collect(Collectors.toList());

        assertEquals(3, results.size());
        assertEquals(stationA, results.get(0));
        assertEquals(stationB, results.get(1));
        assertEquals(stationC, results.get(2));
    }

    @Test
    void shouldGetBoundary() {
        Station stationA = TramStations.Altrincham.fake();
        Station stationB = TramStations.PiccadillyGardens.fake();
        Station stationC = TramStations.Shudehill.fake();

        List<LatLong> result = geography.createBoundaryFor(Stream.of(stationA, stationB, stationC).map(Location::getLatLong));

        assertFalse(result.isEmpty());

        fail("todo how to test this?");
    }
}
