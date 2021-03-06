package com.tramchester.unit.cloud.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.cloud.data.StationDepartureMapper;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        int wait = 42;
        LocalDateTime dueTime = lastUpdate.plusMinutes(wait).truncatedTo(ChronoUnit.MINUTES);
        DueTram dueTram = new DueTram(TramStations.of(Bury), "Due", wait, "Single", dueTime.toLocalTime().minusMinutes(wait));
        final DepartureDTO departureDTO = new DepartureDTO(TramStations.of(NavigationRoad), dueTram, dueTime.toLocalDate());

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
