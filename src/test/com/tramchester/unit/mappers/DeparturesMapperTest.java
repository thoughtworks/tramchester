package com.tramchester.unit.mappers;

import com.tramchester.TestConfig;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Station;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.DepartureListDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.mappers.DeparturesMapper;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeparturesMapperTest {

    private DeparturesMapper mapper;
    private LatLong latLong = new LatLong(-1,2);

    @Before
    public void beforeEachTestRuns() {
        ProvidesNotes providesNotes = new ProvidesNotes(new TestConfig() {
            @Override
            public Path getDataFolder() {
                return null;
            }
        });
        mapper = new DeparturesMapper(providesNotes);
    }

    @Test
    public void shouldConvertOneStationWithDateToDepartuesList() {
        LocalDateTime lastUpdate = LocalDateTime.now();

        StationDepartureInfo departureInfo = new StationDepartureInfo("displayId", "lineName", "platformId",
                "locationName", "message", lastUpdate);
        int wait = 42;
        departureInfo.addDueTram(new DueTram("tramDest", "Due", wait, "Single", lastUpdate.toLocalTime()));
        departureInfo.addDueTram(new DueTram("tramDest", "Due", wait, "Single", lastUpdate.toLocalTime())); // same time, same dest
        departureInfo.addDueTram(new DueTram("tramDest", "NOTDue", wait, "Single", lastUpdate.toLocalTime()));

        List<StationDTO> sourceStations = new LinkedList<>();
        StationDTO stationDTO = new StationDTO(new Station("id", "area", "|stopName", latLong, true), ProximityGroup.ALL);
        sourceStations.add(stationDTO);
        PlatformDTO platformDTO = new PlatformDTO(new Platform("platformId", "Platform name"));
        platformDTO.setDepartureInfo(departureInfo);
        stationDTO.getPlatforms().add(platformDTO);

        SortedSet<DepartureDTO> results = mapper.fromStations(sourceStations);

        assertEquals(1, results.size());

        DepartureDTO first = results.first();
        assertEquals(TramTime.of(lastUpdate.plusMinutes(wait).toLocalTime()), first.getWhen());
        assertEquals("locationName", first.getFrom());
        assertEquals("Single", first.getCarriages());
        assertEquals("Due", first.getStatus());
        assertEquals("tramDest", first.getDestination());
    }

    @Test
    public void shouldKeepUseDestinationToSortWhenTimesSame() {
        LocalDateTime lastUpdateTime = LocalDateTime.now();

        StationDepartureInfo departureInfo = new StationDepartureInfo("displayId", "lineName", "platformId",
                "locationName", "message", lastUpdateTime);
        int wait = 42;

        departureInfo.addDueTram(new DueTram("XX", "Due", wait, "Single", lastUpdateTime.toLocalTime()));
        departureInfo.addDueTram(new DueTram("AA", "Due", wait, "Single", lastUpdateTime.toLocalTime())); // same time, diff dest

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
        TramTime expected = TramTime.of(lastUpdateTime.plusMinutes(wait).toLocalTime());
        assertEquals(expected, first.getWhen());

        DepartureDTO last = results.last();
        assertEquals("XX", last.getDestination());
        assertEquals(expected, last.getWhen());
    }

    @Test
    public void shouldMapDepartureInformationToDepartureListDTOWithNotes() {
        LocalDateTime updateTime = LocalDateTime.now();

        List<StationDepartureInfo> departureInfos = createStationDepartureInfo(updateTime);

        DepartureListDTO result = mapper.from(departureInfos,true);

        SortedSet<DepartureDTO> departures = result.getDepartures();

        assertEquals(3, result.getDepartures().size());
        DepartureDTO first = departures.first();
        assertEquals("destinationC", first.getDestination());
        assertEquals("Departing", first.getStatus());
        assertEquals("location", first.getFrom());
        assertEquals("Double", first.getCarriages());
        assertEquals(TramTime.of(updateTime.plusMinutes(1).toLocalTime()), first.getWhen());

        List<String> notes = result.getNotes();
        assertEquals(2, notes.size());
        assertTrue(notes.contains("'messageOne' - location, Metrolink"));
        assertTrue(notes.contains("'messageTwo' - location, Metrolink"));

    }

    @Test
    public void shouldMapDepartureInformationToDepartureListDTOWithoutNotes() {
        LocalDateTime updateDate = LocalDateTime.now();

        List<StationDepartureInfo> departureInfos = createStationDepartureInfo(updateDate);

        DepartureListDTO result = mapper.from(departureInfos,false);

        SortedSet<DepartureDTO> departures = result.getDepartures();

        assertEquals(3, result.getDepartures().size());
        DepartureDTO first = departures.first();
        assertEquals("destinationC", first.getDestination());
        assertEquals("Departing", first.getStatus());
        assertEquals("location", first.getFrom());
        assertEquals("Double", first.getCarriages());
        assertEquals(TramTime.of(updateDate.plusMinutes(1).toLocalTime()), first.getWhen());

        List<String> notes = result.getNotes();
        assertEquals(0, notes.size());
    }

    private List<StationDepartureInfo> createStationDepartureInfo(LocalDateTime updateDateTime) {
        List<StationDepartureInfo> departureInfos = new LinkedList<>();

        StationDepartureInfo infoA = new StationDepartureInfo("displayId1", "lineName", "stationPlatform1", "location",
                "messageOne", updateDateTime);
        infoA.addDueTram(new DueTram("destinationA", "Due", 5, "Double", updateDateTime.toLocalTime()));
        infoA.addDueTram(new DueTram("destinationB", "Delay", 10, "Single", updateDateTime.toLocalTime()));

        StationDepartureInfo infoB = new StationDepartureInfo("displayId2", "lineName", "stationPlatform2", "location",
                "messageTwo", updateDateTime);
        infoB.addDueTram(new DueTram("destinationC", "Departing", 1, "Double", updateDateTime.toLocalTime()));

        departureInfos.add(infoA);
        departureInfos.add(infoB);
        return departureInfos;
    }
}
