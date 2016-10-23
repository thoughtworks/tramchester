package com.tramchester.domain.presentation.DTO;


import com.tramchester.Stations;
import com.tramchester.domain.RawVehicleStage;
import com.tramchester.domain.RawWalkingStage;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.WalkingStage;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.presentation.VehicleStageWithTiming;
import org.joda.time.LocalTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StageDTOTest {

    @Test
    public void shouldCreateStageDTOCorrectlyForWalking() {
        RawWalkingStage rawWalkingStage = new RawWalkingStage(Stations.Altrincham, Stations.NavigationRoad, 15);
        TransportStage stage = new WalkingStage(rawWalkingStage, 8*60);

        checkValues(stage);
    }

    @Test
    public void shouldCreateStageDTOCorrectlyForTransportStage() {
        RawVehicleStage raw = new RawVehicleStage(Stations.MarketStreet, "routeName",
                TransportMode.Tram, "Displayclass");
        raw.setLastStation(Stations.Bury);
        raw.setCost(42);
        ServiceTime serviceTime = new ServiceTime(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusMinutes(5), "svcId",
                "headSign", "tripId");
        TransportStage stage = new VehicleStageWithTiming(raw, serviceTime, TravelAction.Board);

        checkValues(stage);
    }

    private void checkValues(TransportStage stage) {
        StageDTO dto = new StageDTO(stage);
        assertEquals(stage.getActionStation().getId(), dto.getActionStation().getId());
        assertEquals(stage.isWalk(), dto.isWalk());
        assertEquals(stage.getMode(), dto.getMode());
        assertEquals(stage.getFirstDepartureTime(), dto.getFirstDepartureTime());
        assertEquals(stage.getLastStation().getId(), dto.getLastStation().getId());
        assertEquals(stage.getExpectedArrivalTime(), dto.getExpectedArrivalTime());
        assertEquals(stage.getIsAVehicle(), dto.getIsAVehicle());
        assertEquals(stage.getDuration(), dto.getDuration());
        assertEquals(stage.getFirstStation().getId(), dto.getFirstStation().getId());
        assertEquals(stage.getHeadSign(), dto.getHeadSign());
        assertEquals(stage.getPrompt(), dto.getPrompt());
        assertEquals(stage.getSummary(), dto.getSummary());
        assertEquals(stage.getDisplayClass(), dto.getDisplayClass());
    }


}
