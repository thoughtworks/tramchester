package com.tramchester.unit.mappers;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Station;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.mappers.DeparturesMapper;
import net.sf.cglib.core.Local;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

import static org.junit.Assert.assertEquals;

public class DeparturesMapperTest {

    private DeparturesMapper mapper;
    private LatLong latLong = new LatLong(-1,2);

    @Before
    public void beforeEachTestRuns() {
        mapper = new DeparturesMapper();
    }

    @Test
    public void shouldConvertOneStationWithDateToDepartuesList() {
        DateTime lastUpdate = DateTime.now();

        StationDepartureInfo departureInfo = new StationDepartureInfo("displayId", "lineName", "platformId",
                "locationName", "message", lastUpdate);
        int wait = 42;
        LocalTime lastupdateTime = lastUpdate.toLocalTime();
        departureInfo.addDueTram(new DueTram("tramDest", "Due", wait, "Single", lastupdateTime));
        departureInfo.addDueTram(new DueTram("tramDest", "Due", wait, "Single", lastupdateTime)); // same time, same dest
        departureInfo.addDueTram(new DueTram("tramDest", "NOTDue", wait, "Single", lastupdateTime));

        List<StationDTO> sourceStations = new LinkedList<>();
        StationDTO stationDTO = new StationDTO(new Station("id", "area", "|stopName", latLong, true), ProximityGroup.ALL);
        sourceStations.add(stationDTO);
        PlatformDTO platformDTO = new PlatformDTO(new Platform("platformId", "Platform name"));
        platformDTO.setDepartureInfo(departureInfo);
        stationDTO.getPlatforms().add(platformDTO);

        SortedSet<DepartureDTO> results = mapper.fromStations(sourceStations);

        assertEquals(1, results.size());

        DepartureDTO first = results.first();
        assertEquals(TramTime.create(lastupdateTime.plusMinutes(wait)), first.getWhen());
        assertEquals("locationName", first.getFrom());
        assertEquals("Single", first.getCarriages());
        assertEquals("Due", first.getStatus());
        assertEquals("tramDest", first.getDestination());
    }

    @Test
    public void shouldKeepUseDestinationToSortWhenTimesSame() {
        DateTime lastUpdateTime = DateTime.now();

        StationDepartureInfo departureInfo = new StationDepartureInfo("displayId", "lineName", "platformId",
                "locationName", "message", lastUpdateTime);
        int wait = 42;
        LocalTime lastUpdate = lastUpdateTime.toLocalTime();

        departureInfo.addDueTram(new DueTram("XX", "Due", wait, "Single", lastUpdate));
        departureInfo.addDueTram(new DueTram("AA", "Due", wait, "Single", lastUpdate)); // same time, diff dest

        List<StationDTO> sourceStations = new LinkedList<>();
        StationDTO stationDTO = new StationDTO(new Station("id", "area", "|stopName", latLong, true), ProximityGroup.ALL);
        sourceStations.add(stationDTO);
        PlatformDTO platformDTO = new PlatformDTO(new Platform("platformId", "Platform name"));
        platformDTO.setDepartureInfo(departureInfo);
        stationDTO.getPlatforms().add(platformDTO);

        SortedSet<DepartureDTO> results = mapper.fromStations(sourceStations);

        assertEquals(2, results.size());

        DepartureDTO first = results.first();
        assertEquals("AA", first.getDestination());
        TramTime expected = TramTime.create(lastUpdate.plusMinutes(wait));
        assertEquals(expected, first.getWhen());

        DepartureDTO last = results.last();
        assertEquals("XX", last.getDestination());
        assertEquals(expected, last.getWhen());
    }
}
