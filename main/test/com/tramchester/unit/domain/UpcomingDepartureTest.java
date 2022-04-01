package com.tramchester.unit.domain;

import com.tramchester.domain.Agency;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class UpcomingDepartureTest {

    private Station destination;
    private Station displayLocation;
    private final Agency agency = TestEnv.MetAgency();

    @BeforeEach
    void setUp() {
        destination = TramStations.Bury.fake();
        displayLocation = TramStations.PiccadillyGardens.fake();
    }

    @Test
    void calculateWhenCorrectly() {
        LocalTime updateTime = LocalTime.of(10,42);
        UpcomingDeparture dueTram = new UpcomingDeparture(displayLocation, destination, "Due",
                Duration.ofMinutes(4), "Double", updateTime, agency);

        TramTime result = dueTram.getWhen();
        assertEquals(updateTime.plusMinutes(4), result.asLocalTime());
        assertFalse(result.isNextDay());
        assertEquals(displayLocation, dueTram.getDisplayLocation());
        assertEquals(destination, dueTram.getDestination());
        assertEquals("Due", dueTram.getStatus());
        assertEquals("Double", dueTram.getCarriages());

    }

    @Test
    void calculateWhenCorrectAcrossMidnight() {
        LocalTime updateTime = LocalTime.of(23,58);
        UpcomingDeparture dueTram = new UpcomingDeparture(displayLocation, destination, "Due",
                Duration.ofMinutes(4), "Double", updateTime, agency);

        TramTime result = dueTram.getWhen();
        assertEquals(LocalTime.of(0,2), result.asLocalTime());
        assertTrue(result.isNextDay());
    }
}
