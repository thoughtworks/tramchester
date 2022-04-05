package com.tramchester.unit.domain;

import com.tramchester.domain.Agency;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UpcomingDepartureTest {

    private Station destination;
    private Station displayLocation;
    private final Agency agency = TestEnv.MetAgency();
    private final TransportMode mode = TransportMode.Tram;

    @BeforeEach
    void setUp() {
        destination = TramStations.Bury.fake();
        displayLocation = TramStations.PiccadillyGardens.fake();
    }

    @Test
    void calculateWhenCorrectly() {
        LocalTime updateTime = LocalTime.of(10,42);
        LocalDate date = LocalDate.of(2022,4,30);
        UpcomingDeparture dueTram = new UpcomingDeparture(date, displayLocation, destination, "Due",
                Duration.ofMinutes(4), "Double", updateTime, agency, mode);

        TramTime result = dueTram.getWhen();
        assertEquals(updateTime.plusMinutes(4), result.asLocalTime());
        assertFalse(result.isNextDay());
        assertEquals(displayLocation, dueTram.getDisplayLocation());
        assertEquals(destination, dueTram.getDestination());
        assertEquals("Due", dueTram.getStatus());
        assertEquals("Double", dueTram.getCarriages());
        assertEquals(TransportMode.Tram, dueTram.getMode());
        assertEquals(agency, dueTram.getAgency());
        assertEquals(date, dueTram.getDate());

    }

    @Test
    void calculateWhenCorrectAcrossMidnight() {
        LocalTime updateTime = LocalTime.of(23,58);
        LocalDate date = LocalDate.of(2022,4,30);

        UpcomingDeparture dueTram = new UpcomingDeparture(date, displayLocation, destination, "Due",
                Duration.ofMinutes(4), "Double", updateTime, agency, mode);

        TramTime result = dueTram.getWhen();
        assertEquals(LocalTime.of(0,2), result.asLocalTime());
        assertTrue(result.isNextDay());
    }
}
