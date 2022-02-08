package com.tramchester.unit.mappers;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Location;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.mappers.Geography;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tec.units.ri.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Time;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GeographyTest {
    private Geography geography;
    private TramchesterConfig config;

    @BeforeEach
    void onceBeforeEachTest() {
        config = TestEnv.GET();
        geography = new Geography(config);
    }

    @Test
    void shouldGetWalkingTime() {

        Location<?> start = TramStations.StPetersSquare.fake();
        Location<?> end = TramStations.PiccadillyGardens.fake();

        int expected = CoordinateTransforms.calcCostInMinutes(start, end, config.getWalkingMPH());

        Quantity<Time> result = geography.getWalkingTime(TestEnv.MetersBetweenStPeterSqAndPiccGardens);

        assertEquals(expected, Math.ceil(result.to(Units.MINUTE).getValue().doubleValue()));
    }

    @Test
    void shouldGetWalkingTimeInMins() {

        Location<?> start = TramStations.StPetersSquare.fake();
        Location<?> end = TramStations.PiccadillyGardens.fake();

        int expected = CoordinateTransforms.calcCostInMinutes(start, end, config.getWalkingMPH());

        int result = geography.getWalkingTimeInMinutes(TestEnv.MetersBetweenStPeterSqAndPiccGardens);

        assertEquals(expected, result);
    }
}
