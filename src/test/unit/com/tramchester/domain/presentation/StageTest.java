package com.tramchester.domain.presentation;


import com.tramchester.domain.RawStage;
import junit.framework.Assert;
import org.junit.Test;

import java.time.LocalTime;
import java.util.Arrays;

import static junit.framework.TestCase.assertEquals;

public class StageTest {

    private String tripId = "tripId";

    @Test
    public void shouldGetDurationCorrectly() {
        ServiceTime serviceTime = new ServiceTime(LocalTime.of(8, 00), LocalTime.of(9, 15), "svcId", "headsign", 30, tripId);
        Stage stage = new Stage(new RawStage("firstStation", "route", "Tram", "cssClass"), Arrays.asList(serviceTime));

        assertEquals(75, stage.getDuration());
    }

    @Test
    public void shouldGetFirstDepartureAndFirstArrival() {
        ServiceTime serviceTimeA = new ServiceTime(LocalTime.of(8, 00), LocalTime.of(9, 15), "svcId", "headsign", 30, tripId);
        ServiceTime serviceTimeB = new ServiceTime(LocalTime.of(7, 00), LocalTime.of(7, 45), "svcId", "headsign", 7*60, tripId);

        Stage stage = new Stage(new RawStage("firstStation", "route", "Tram", "cssClass"),
                Arrays.asList(serviceTimeA,serviceTimeB));

        assertEquals(LocalTime.of(8, 00), stage.getFirstDepartureTime());
        assertEquals(LocalTime.of(9, 15), stage.getExpectedArrivalTime());
    }

    @Test
    public void shouldGetEarliestDepartCorrectly() {
        ServiceTime serviceTimeA = new ServiceTime(LocalTime.of(8, 00), LocalTime.of(9, 15), "svcId", "headsign", 8*60, tripId);
        ServiceTime serviceTimeB = new ServiceTime(LocalTime.of(7, 00), LocalTime.of(7, 45), "svcId", "headsign", 7*60, tripId);

        Stage stage = new Stage(new RawStage("firstStation", "route", "Tram", "cssClass"),
                Arrays.asList(serviceTimeA,serviceTimeB));

        assertEquals(7*60, stage.findEarliestDepartureTime());
    }

    @Test
    public void shouldGetDurationCorrectlyWhenAfterMidnight() {
        ServiceTime serviceTime = new ServiceTime(LocalTime.of(23, 50), LocalTime.of(0, 15), "svcId", "headsign", 30, tripId);
        Stage stage = new Stage(new RawStage("firstStation", "route", "Tram", "cssClass"), Arrays.asList(serviceTime));

        assertEquals(25, stage.getDuration());
    }

    @Test
    public void shouldGetAttributesPassIn() {
        ServiceTime serviceTime = new ServiceTime(LocalTime.of(23, 50), LocalTime.of(0, 15), "svcId", "headsign", 30, tripId);

        RawStage rawStage = new RawStage("firstStation", "route", "Tram", "cssClass");
        rawStage.setLastStation("lastStation");
        rawStage.setServiceId("svcId");
        Stage stage = new Stage(rawStage, Arrays.asList(serviceTime));
        assertEquals("cssClass", stage.getTramRouteId());
        assertEquals("Tram", stage.getMode());
        assertEquals("route", stage.getRoute());
        assertEquals("firstStation", stage.getFirstStation());
        assertEquals("lastStation", stage.getLastStation());
        assertEquals("svcId", stage.getServiceId());
    }
}
