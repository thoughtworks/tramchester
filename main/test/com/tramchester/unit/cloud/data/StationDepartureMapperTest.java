package com.tramchester.unit.cloud.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.cloud.data.StationDepartureMapper;
import com.tramchester.livedata.domain.liveUpdates.DueTram;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.domain.DTO.StationDepartureInfoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.tramchester.testSupport.reference.TramStations.Bury;
import static com.tramchester.testSupport.reference.TramStations.NavigationRoad;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StationDepartureMapperTest {
    private static final String json = "[{\"lineName\":\"lineName\",\"stationPlatform\":\"platforId\",\"message\":\"messageTxt\"," +
            "\"dueTrams\":[{\"carriages\":\"Single\",\"destination\":\"Bury\",\"dueTime\":\"2018-11-15T15:48:00\"," +
            "\"from\":\"Navigation Road\",\"status\":\"Due\",\"wait\":42,\"when\":\"15:48\"}]," +
            "\"lastUpdate\":\"2018-11-15T15:06:32\",\"displayId\":\"displayId\",\"location\":\"Navigation Road\"}]";

    private StationDepartureMapper mapper;
    private List<StationDepartureInfoDTO> departures;

    @BeforeEach
    void beforeEachTestRuns() {
        ObjectMapper objectMapper = new ObjectMapper();
        mapper = new StationDepartureMapper(objectMapper);
        departures = new ArrayList<>();
        LocalDateTime lastUpdate = LocalDateTime.of(2018,11,15,15,6,32);

        Duration wait = Duration.ofMinutes(42);
        LocalDateTime dueTime = lastUpdate.plus(wait).truncatedTo(ChronoUnit.MINUTES);
        DueTram dueTram = new DueTram(Bury.fake(), "Due", wait, "Single", dueTime.toLocalTime().minus(wait));
        final DepartureDTO departureDTO = new DepartureDTO(NavigationRoad.fake(), dueTram, dueTime.toLocalDate());

        List<DepartureDTO> dueTrams = Collections.singletonList(departureDTO);
        departures.add(new StationDepartureInfoDTO("lineName", "platforId",
                "messageTxt", dueTrams, lastUpdate, "displayId",  "Navigation Road"));
    }

    @Test
    void shoudCreateJson() throws JsonProcessingException {
        String json = mapper.map(departures);
        assertEquals(StationDepartureMapperTest.json, json);

    }

    @Test
    void shouldParseJson() {
        List<StationDepartureInfoDTO> results = mapper.parse(json);
        assertEquals(departures, results);
    }
}
