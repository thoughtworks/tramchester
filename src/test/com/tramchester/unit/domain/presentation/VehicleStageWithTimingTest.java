package com.tramchester.unit.domain.presentation;


import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.presentation.VehicleStageWithTiming;
import com.tramchester.integration.Stations;
import com.tramchester.livedata.EnrichPlatform;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VehicleStageWithTimingTest {

    private String tripId = "tripId";
    Location firstStation = new Station("firstStation", "area", "name", new LatLong(1,1), true);
    RawVehicleStage tramRawStage = new RawVehicleStage(firstStation, "route", TransportMode.Tram, "cssClass");

    @Test
    public void shouldGetDurationCorrectly() {
        ServiceTime serviceTime = new ServiceTime(new LocalTime(8, 00), new LocalTime(9, 15), "svcId", "headsign", tripId);
        VehicleStageWithTiming stage = new VehicleStageWithTiming(tramRawStage, serviceTime, TravelAction.Board);
        stage.setCost(75);

        assertEquals(75, stage.getDuration());
    }

    @Test
    public void shouldGetAsADTO() {
        ServiceTime serviceTime = new ServiceTime(new LocalTime(8, 00), new LocalTime(9, 15), "svcId", "headsign", tripId);
        VehicleStageWithTiming stage = new VehicleStageWithTiming(tramRawStage, serviceTime, TravelAction.Board);
        stage.setCost(75);
        stage.setLastStation(Stations.Bury);
        Platform platform = new Platform("platformA1", "station platform 1");
        stage.setPlatform(platform);

        StationDepartureInfo departureInfo = new StationDepartureInfo("lineName","stationPlatform",
                "message", DateTime.now());

        EnrichPlatform liveDataEnricher = aPlatform -> {
            aPlatform.setDepartureInfo(departureInfo);
        };
        StageDTO dto = stage.asDTO(liveDataEnricher);
        assertEquals(stage.getSummary(), dto.getSummary());
        assertEquals(stage.getExpectedArrivalTime(), dto.getExpectedArrivalTime());
        assertEquals(stage.getHeadSign(), dto.getHeadSign());
        assertEquals(stage.getActionStation().getId(),dto.getActionStation().getId());
        assertEquals(stage.getDuration(),dto.getDuration());
        assertEquals(stage.getFirstStation().getId(),dto.getFirstStation().getId());
        assertEquals(stage.getLastStation().getId(), dto.getLastStation().getId());
        assertEquals(stage.getIsAVehicle(),dto.getIsAVehicle());
        assertEquals(stage.getFirstDepartureTime(),dto.getFirstDepartureTime());
        assertEquals(stage.getPrompt(),dto.getPrompt());
        assertEquals(stage.isWalk(), dto.isWalk());
        assertEquals(stage.getMode(), dto.getMode());
        assertTrue(dto.getHasPlatform());
        assertEquals(platform.getId(), dto.getPlatform().getId());
        assertEquals(platform.getName(), dto.getPlatform().getName());
        assertEquals(platform.getPlatformNumber(), dto.getPlatform().getPlatformNumber());
        assertEquals(departureInfo.getMessage(), dto.getPlatform().getStationDepartureInfo().getMessage());
    }

    @Test
    public void shouldGetFirstDepartureAndFirstArrival() {
        ServiceTime serviceTimeA = new ServiceTime(new LocalTime(8, 00), new LocalTime(9, 15), "svcId", "headsign", tripId);

        VehicleStageWithTiming stage = new VehicleStageWithTiming(tramRawStage, serviceTimeA, TravelAction.Board);

        assertEquals(new LocalTime(8, 00), stage.getFirstDepartureTime());
        assertEquals(new LocalTime(9, 15), stage.getExpectedArrivalTime());
    }

    @Test
    public void shouldGetAttributesPassedIn() {
        ServiceTime serviceTime = new ServiceTime(new LocalTime(23, 50), new LocalTime(0, 15), "svcId", "headsign", tripId);

        RawVehicleStage rawTravelStage = new RawVehicleStage(firstStation, "route", TransportMode.Tram, "cssClass");
        Location lastStation = new Station("lastStation", "area", "name", new LatLong(-1, -1), true);
        rawTravelStage.setLastStation(lastStation);
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
    }

    @Test
    public void shouldDisplayCorrectPrompt() throws TramchesterException {
        ServiceTime serviceTime = new ServiceTime(new LocalTime(23, 50), new LocalTime(0, 15), "svcId", "headsign", tripId);
        RawVehicleStage rawTravelStage = new RawVehicleStage(firstStation, "route", TransportMode.Tram, "cssClass");

        VehicleStageWithTiming stage = new VehicleStageWithTiming(rawTravelStage, serviceTime, TravelAction.Board);

        assertEquals(stage.getPrompt(),"Board tram at");
    }

    @Test
    public void shouldGetStageSummary() throws TramchesterException {
        ServiceTime serviceTime = new ServiceTime(new LocalTime(23, 50), new LocalTime(0, 15), "svcId", "headsign", tripId);

        RawVehicleStage rawTravelStageA = new RawVehicleStage(firstStation, "routeName", TransportMode.Tram, "cssClass");
        VehicleStageWithTiming stageA = new VehicleStageWithTiming(rawTravelStageA, serviceTime, TravelAction.Board);
        String result = stageA.getSummary();

        assertEquals(result, "routeName Tram line");

        RawVehicleStage rawTravelStageB = new RawVehicleStage(firstStation, "routeName", TransportMode.Bus, "cssClass");
        VehicleStageWithTiming stageB = new VehicleStageWithTiming(rawTravelStageB, serviceTime, TravelAction.Board);
        result = stageB.getSummary();

        assertEquals(result, "routeName Bus route");
    }

    @Test
    public void shouldGetCorrectPromptForStage() throws TramchesterException {
        ServiceTime serviceTime = new ServiceTime(new LocalTime(23, 50), new LocalTime(0, 15), "svcId", "headsign", tripId);

        VehicleStageWithTiming tramStageBoard = new VehicleStageWithTiming(tramRawStage, serviceTime, TravelAction.Board);
        VehicleStageWithTiming tramStageLeave = new VehicleStageWithTiming(tramRawStage, serviceTime, TravelAction.Leave);
        VehicleStageWithTiming tramStageChange = new VehicleStageWithTiming(tramRawStage, serviceTime, TravelAction.Change);

        assertEquals("Board tram at", tramStageBoard.getPrompt());
        assertEquals("Leave tram at", tramStageLeave.getPrompt());
        assertEquals("Change tram at", tramStageChange.getPrompt());

        RawVehicleStage busRawStage = new RawVehicleStage(firstStation, "route", TransportMode.Bus, "cssClass");

        VehicleStageWithTiming busStageBoard = new VehicleStageWithTiming(busRawStage, serviceTime, TravelAction.Board);
        VehicleStageWithTiming busStageLeave = new VehicleStageWithTiming(busRawStage, serviceTime, TravelAction.Leave);
        VehicleStageWithTiming busStageChange = new VehicleStageWithTiming(busRawStage, serviceTime, TravelAction.Change);

        assertEquals("Board bus at", busStageBoard.getPrompt());
        assertEquals("Leave bus at", busStageLeave.getPrompt());
        assertEquals("Change bus at", busStageChange.getPrompt());

    }
}
