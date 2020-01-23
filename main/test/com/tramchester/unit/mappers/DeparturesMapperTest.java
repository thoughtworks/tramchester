package com.tramchester.unit.mappers;

import com.tramchester.TestConfig;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Station;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.integration.Stations;
import com.tramchester.mappers.DeparturesMapper;
import com.tramchester.repository.LiveDataRepository;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeparturesMapperTest extends EasyMockSupport {

    private DeparturesMapper mapper;
    private LatLong latLong = new LatLong(-1,2);

    @Before
    public void beforeEachTestRuns() {
        LiveDataRepository liveDataRepository = createStrictMock(LiveDataRepository.class);
        ProvidesNotes providesNotes = new ProvidesNotes(TestConfig.GET(), liveDataRepository);
        mapper = new DeparturesMapper(providesNotes, liveDataRepository);
    }

    @Test
    public void shouldConvertOneStationWithDateToDepartuesList() {
        LocalDateTime lastUpdate = LocalDateTime.now();

        int wait = 42;
        List<DepartureDTO> dueTrams = new ArrayList<>();
        dueTrams.add(new DepartureDTO("locationName", new DueTram(Stations.Bury, "Due", wait, "Single", lastUpdate.toLocalTime())));
        dueTrams.add(new DepartureDTO("locationName", new DueTram(Stations.Bury, "Due", wait, "Single", lastUpdate.toLocalTime()))); // same time, same dest
        dueTrams.add(new DepartureDTO("locationName", new DueTram(Stations.Bury, "NOTDue", wait, "Single", lastUpdate.toLocalTime())));

        StationDepartureInfoDTO departureInfo = new StationDepartureInfoDTO("lineName", "platformId", "some message",
                dueTrams, lastUpdate, "displayId", "locationName");

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
        assertEquals("Bury", first.getDestination());
    }

    @Test
    public void shouldKeepUseDestinationToSortWhenTimesSame() {
        LocalDateTime lastUpdate = LocalDateTime.now();

        int wait = 42;
        List<DepartureDTO> dueTrams = new ArrayList<>();
        dueTrams.add(new DepartureDTO("locationName", new DueTram(Stations.Bury, "Due", wait, "Single", lastUpdate.toLocalTime())));
        dueTrams.add(new DepartureDTO("locationName", new DueTram(Stations.Rochdale, "Due", wait, "Single",
                lastUpdate.toLocalTime()))); // same time, diff dest

        StationDepartureInfoDTO departureInfo = new StationDepartureInfoDTO("lineName", "platformId", "some message",
                dueTrams, lastUpdate, "displayId", "locationName");

        List<StationDTO> sourceStations = new LinkedList<>();
        StationDTO stationDTO = new StationDTO(new Station("id", "area", "|stopName", latLong, true), ProximityGroup.ALL);
        sourceStations.add(stationDTO);
        PlatformDTO platformDTO = new PlatformDTO(new Platform("platformId", "Platform name"));
        platformDTO.setDepartureInfo(departureInfo);
        stationDTO.getPlatforms().add(platformDTO);

        SortedSet<DepartureDTO> results = mapper.fromStations(sourceStations);

        assertEquals(2, results.size());

        DepartureDTO first = results.first();
        assertEquals("Bury", first.getDestination());
        TramTime expected = TramTime.of(lastUpdate.plusMinutes(wait).toLocalTime());
        assertEquals(expected, first.getWhen());

        DepartureDTO last = results.last();
        assertEquals("Rochdale Interchange", last.getDestination());
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
        assertEquals("Cornbrook", first.getDestination());
        assertEquals("Departing", first.getStatus());
        assertEquals("Navigation Road", first.getFrom());
        assertEquals("Double", first.getCarriages());
        assertEquals(TramTime.of(updateTime.plusMinutes(1).toLocalTime()), first.getWhen());

        List<String> notes = result.getNotes();
        assertEquals(2, notes.size());
        assertTrue(notes.contains("'messageOne' - Navigation Road, Metrolink"));
        assertTrue(notes.contains("'messageTwo' - Navigation Road, Metrolink"));

    }

    @Test
    public void shouldMapDepartureInformationToDepartureListDTOWithoutNotes() {
        LocalDateTime updateDate = LocalDateTime.now();

        List<StationDepartureInfo> departureInfos = createStationDepartureInfo(updateDate);

        DepartureListDTO result = mapper.from(departureInfos,false);

        SortedSet<DepartureDTO> departures = result.getDepartures();

        assertEquals(3, result.getDepartures().size());
        DepartureDTO first = departures.first();
        assertEquals("Cornbrook", first.getDestination());
        assertEquals("Departing", first.getStatus());
        assertEquals("Navigation Road", first.getFrom());
        assertEquals("Double", first.getCarriages());
        assertEquals(TramTime.of(updateDate.plusMinutes(1).toLocalTime()), first.getWhen());

        List<String> notes = result.getNotes();
        assertEquals(0, notes.size());
    }

    private List<StationDepartureInfo> createStationDepartureInfo(LocalDateTime updateDateTime) {
        List<StationDepartureInfo> departureInfos = new LinkedList<>();

        StationDepartureInfo infoA = new StationDepartureInfo("displayId1", "lineName",
                StationDepartureInfo.Direction.Incoming, "stationPlatform1", Stations.NavigationRoad,
                "messageOne", updateDateTime);
        infoA.addDueTram(new DueTram(Stations.Altrincham, "Due", 5, "Double", updateDateTime.toLocalTime()));
        infoA.addDueTram(new DueTram(Stations.Bury, "Delay", 10, "Single", updateDateTime.toLocalTime()));

        StationDepartureInfo infoB = new StationDepartureInfo("displayId2", "lineName",
                StationDepartureInfo.Direction.Incoming, "stationPlatform2", Stations.NavigationRoad,
                "messageTwo", updateDateTime);
        infoB.addDueTram(new DueTram(Stations.Cornbrook, "Departing", 1, "Double", updateDateTime.toLocalTime()));

        departureInfos.add(infoA);
        departureInfos.add(infoB);
        return departureInfos;
    }
}
