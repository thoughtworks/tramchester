package com.tramchester.unit.cloud.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.cloud.data.StationDepartureMapper;
import com.tramchester.domain.Agency;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.domain.DTO.StationDepartureInfoDTO;
import com.tramchester.livedata.domain.DTO.archived.ArchivedDepartureDTO;
import com.tramchester.livedata.domain.DTO.archived.ArchivedStationDepartureInfoDTO;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.testSupport.TestEnv;
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
    private LocalDateTime lastUpdate;

    @BeforeEach
    void beforeEachTestRuns() {
        ObjectMapper objectMapper = new ObjectMapper();
        mapper = new StationDepartureMapper(objectMapper);
        departures = new ArrayList<>();
        lastUpdate = LocalDateTime.of(2018,11,15,15,6,32);

        Duration wait = Duration.ofMinutes(42);
        LocalDateTime dueTime = lastUpdate.plus(wait).truncatedTo(ChronoUnit.MINUTES);
        Agency agency = TestEnv.MetAgency();

        UpcomingDeparture dueTram = new UpcomingDeparture(lastUpdate.toLocalDate(), NavigationRoad.fake(), Bury.fake(),
                "Due", TramTime.ofHourMins(dueTime.toLocalTime()), "Single",agency, TransportMode.Tram);

        final DepartureDTO departureDTO = new DepartureDTO(NavigationRoad.fake(), dueTram, lastUpdate);

        List<DepartureDTO> dueTrams = Collections.singletonList(departureDTO);
        departures.add(new StationDepartureInfoDTO("lineName", "platforId",
                "messageTxt", dueTrams, lastUpdate, "displayId",  "Navigation Road"));
    }

    @Test
    void shoudCreateJson() throws JsonProcessingException {
        String result = mapper.map(departures);
        assertEquals(json, result, result.toString());
    }

    @Test
    void shouldParseJson() {
        List<ArchivedStationDepartureInfoDTO> results = mapper.parse(json);
        assertEquals(1, results.size());

        ArchivedStationDepartureInfoDTO departureInfoDTO = results.get(0);
        assertEquals("lineName", departureInfoDTO.getLineName());
        assertEquals("platforId", departureInfoDTO.getStationPlatform());
        assertEquals("messageTxt", departureInfoDTO.getMessage());
        assertEquals(lastUpdate, departureInfoDTO.getLastUpdate());

        List<ArchivedDepartureDTO> trams = departureInfoDTO.getDueTrams();
        assertEquals(1, trams.size());
    }
}
