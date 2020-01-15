package com.tramchester.unit.domain.presentation;


import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.presentation.VehicleStageWithTiming;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class VehicleStageWithTimingTest {

    private String tripId = "tripId";
    private Location firstStation = new Station("firstStation", "area", "name", new LatLong(1,1), true);
    private RawVehicleStage tramRawStage = new RawVehicleStage(firstStation, "route", TransportMode.Tram, "cssClass");

    @Test
    public void shouldGetDurationCorrectly() throws TramchesterException {
        ServiceTime serviceTime = new ServiceTime(TramTime.of(8, 00), TramTime.of(9, 15), "svcId", "headsign", tripId);
        VehicleStageWithTiming stage = new VehicleStageWithTiming(tramRawStage, serviceTime, TravelAction.Board);
        stage.setCost(75);

        assertEquals(75, stage.getDuration());
    }

    @Test
    public void shouldGetFirstDepartureAndFirstArrival() throws TramchesterException {
        ServiceTime serviceTimeA = new ServiceTime(TramTime.of(8, 00), TramTime.of(9, 15), "svcId", "headsign", tripId);

        VehicleStageWithTiming stage = new VehicleStageWithTiming(tramRawStage, serviceTimeA, TravelAction.Board);

        assertEquals(TramTime.of(8, 00), stage.getFirstDepartureTime());
        assertEquals(TramTime.of(9, 15), stage.getExpectedArrivalTime());
    }

    @Test
    public void shouldGetAttributesPassedIn() throws TramchesterException {
        ServiceTime serviceTime = new ServiceTime(TramTime.of(23, 50), TramTime.of(0, 15), "svcId", "headsign", tripId);

        RawVehicleStage rawTravelStage = new RawVehicleStage(firstStation, "route", TransportMode.Tram, "cssClass");
        Location lastStation = new Station("lastStation", "area", "name", new LatLong(-1, -1), true);
        rawTravelStage.setLastStation(lastStation,55);
        rawTravelStage.setServiceId("svcId");
        rawTravelStage.setCost(25);
        VehicleStageWithTiming stage = new VehicleStageWithTiming(rawTravelStage, serviceTime, TravelAction.Board);
        assertEquals("cssClass", stage.getDisplayClass());
        assertEquals(TransportMode.Tram, stage.getMode());
        assertEquals("route", stage.getRouteName());
        assertEquals(firstStation, stage.getFirstStation());
        assertEquals(lastStation, stage.getLastStation());
        assertEquals(firstStation, stage.getActionStation());
        assertEquals("svcId", stage.getServiceId());
        assertEquals("headsign", stage.getHeadSign());
        assertEquals(55, stage.getPassedStops());
    }

}
