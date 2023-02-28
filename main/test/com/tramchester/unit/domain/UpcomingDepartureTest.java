package com.tramchester.unit.domain;

import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

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
    void populateCorrectly() {
        LocalTime updateTime = LocalTime.of(10,42);
        TramTime when = TramTime.ofHourMins(updateTime.plusMinutes(4));
        LocalDate date = LocalDate.of(2022,4,30);

        UpcomingDeparture departure = new UpcomingDeparture(date, displayLocation, destination, "Due",
                when, "Double",  agency, mode);

        TramTime result = departure.getWhen();
        assertEquals(updateTime.plusMinutes(4), result.asLocalTime());
        assertFalse(result.isNextDay());
        assertEquals(displayLocation, departure.getDisplayLocation());
        assertEquals(destination, departure.getDestination());
        assertEquals("Due", departure.getStatus());
        assertEquals("Double", departure.getCarriages());
        assertEquals(TransportMode.Tram, departure.getMode());
        assertEquals(agency, departure.getAgency());
        assertEquals(date, departure.getDate());

        assertFalse(departure.hasPlatform());

        LatLong nearBury = TestEnv.stPetersSquareLocation();
        Platform platform = MutablePlatform.buildForTFGMTram("id", TramStations.Bury.fake(), nearBury, DataSourceID.tfgm,
                NaptanArea.createId("areaId1"));
        departure.setPlatform(platform);

        assertTrue(departure.hasPlatform());
        assertEquals(platform, departure.getPlatform());

    }

    @Test
    void calculateWhenCorrectAcrossMidnight() {
        LocalTime updateTime = LocalTime.of(23,58);
        LocalDate date = LocalDate.of(2022,4,30);
        TramTime when = TramTime.ofHourMins(updateTime).plusMinutes(4);

        UpcomingDeparture dueTram = new UpcomingDeparture(date, displayLocation, destination, "Due",
                when, "Double",  agency, mode);

        TramTime result = dueTram.getWhen();
        assertEquals(LocalTime.of(0,2), result.asLocalTime());
        assertTrue(result.isNextDay());
    }
}
