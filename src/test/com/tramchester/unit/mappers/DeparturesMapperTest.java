package com.tramchester.unit.mappers;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Station;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.mappers.DeparturesMapper;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DeparturesMapperTest {


    @Test
    public void shouldConvertOneStationWithDateToDepartuesList() {
        DeparturesMapper mapper = new DeparturesMapper();
        DateTime lastUpdate = DateTime.now();

        LatLong latLong = new LatLong(-1,2);

        StationDepartureInfo departureInfo = new StationDepartureInfo("displayId", "lineName", "platformId",
                "locationName", "message", lastUpdate);
        int wait = 42;
        departureInfo.addDueTram(new DueTram("tramDest", "Due", wait, "Single", lastUpdate));

        List<StationDTO> sourceStations = new LinkedList<>();
        StationDTO stationDTO = new StationDTO(new Station("id", "area", "|stopName", latLong, true), ProximityGroup.ALL);
        sourceStations.add(stationDTO);
        PlatformDTO platformDTO = new PlatformDTO(new Platform("platformId", "Platform name"));
        platformDTO.setDepartureInfo(departureInfo);
        stationDTO.getPlatforms().add(platformDTO);

        List<DepartureDTO> results = mapper.fromStations(sourceStations);

        assertEquals(1, results.size());

        DepartureDTO departureDTO = results.get(0);
        assertEquals(lastUpdate.plusMinutes(wait).toLocalTime(), departureDTO.getWhen());
        assertEquals("locationName", departureDTO.getFrom());
        assertEquals("Single", departureDTO.getCarriages());
        assertEquals("Due", departureDTO.getStatus());
        assertEquals("tramDest", departureDTO.getDestination());

    }
}
