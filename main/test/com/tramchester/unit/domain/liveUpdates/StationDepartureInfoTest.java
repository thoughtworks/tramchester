package com.tramchester.unit.domain.liveUpdates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.Assert.assertEquals;

public class StationDepartureInfoTest {

    @Test
    public void ShouldRoundTripSerialisation() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        LocalDateTime lastUpdate = LocalDateTime.now();
        StationDepartureInfo info = new StationDepartureInfo("displayId", "lineName", "stationPlatform", "location",
                "message", lastUpdate);
        int wait = 42;
        LocalTime updateTime = lastUpdate.toLocalTime();
        info.addDueTram(new DueTram("destination", "status", wait, "carriages", updateTime));

        String serial = mapper.writeValueAsString(info);

        StationDepartureInfo result = mapper.readValue(serial, StationDepartureInfo.class);

        assertEquals(info, result);
    }
}
