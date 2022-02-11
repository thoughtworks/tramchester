package com.tramchester.unit.mappers;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
