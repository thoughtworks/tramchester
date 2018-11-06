package com.tramchester.unit.domain;

import com.tramchester.TestConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.integration.Stations;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ProvidesNotesTest {
    private ProvidesNotes provider;
    private SortedSet<JourneyDTO> decoratedJourneys;
    String ecclesWebSite = "Please check <a href=\"https://tfgm.com/travel-updates/eccles-line\">TFGM</a> for details.";


    @Before
    public void beforeEachTestRuns() {
        decoratedJourneys = new TreeSet<>();
        provider = new ProvidesNotes(new TestConfig() {
            @Override
            public Path getDataFolder() {
                return null;
            }
        });
    }

    @Test
    public void shouldAddNotesForClosedStations() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,29));
        List<String> result = provider.createNotesForJourneys(queryDate, decoratedJourneys);

        assertThat(result, hasItem("St Peters Square is currently closed. "+ProvidesNotes.website));
    }

    @Test
    public void shouldAddNotesForSaturdayJourney() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,29));
        List<String> result = provider.createNotesForJourneys(queryDate, decoratedJourneys);

        assertThat(result, hasItem(ProvidesNotes.weekend));
    }

    @Test
    public void shouldAddNotesForSundayJourney() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,30));
        List<String> result = provider.createNotesForJourneys(queryDate, decoratedJourneys);

        assertThat(result, hasItem(ProvidesNotes.weekend));
    }

    @Test
    public void shouldNotShowNotesOnOtherDay() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,31));
        List<String> result = provider.createNotesForJourneys(queryDate, decoratedJourneys);

        assertThat(result, not(hasItem(ProvidesNotes.weekend)));
    }

    @Test
    public void shouldHaveNoteForChristmasServices() {
        int year = 2018;
        LocalDate date = LocalDate.of(year, 12, 23);

        List<String> result = provider.createNotesForJourneys(new TramServiceDate(date), decoratedJourneys);
        assertThat(result, not(hasItem(ProvidesNotes.christmas)));

        for(int offset=1; offset<11; offset++) {
            TramServiceDate queryDate = new TramServiceDate(date.plusDays(offset));
            result = provider.createNotesForJourneys(queryDate, decoratedJourneys);
            assertThat(queryDate.toString(), result, hasItem(ProvidesNotes.christmas));
        }

        date = LocalDate.of(year+1, 1, 3);
        result = provider.createNotesForJourneys(new TramServiceDate(date), decoratedJourneys);
        assertThat(result, not(hasItem(ProvidesNotes.christmas)));
    }

    @Test
    public void shouldNotAddMessageIfNotMessageForJourney() {
        List<StageDTO> stages = new LinkedList<>();

        String text = "<no message>";
        StageDTO stageA = createStage(TransportMode.Tram, "platformLocation", "platformIdA", text, "displayUnitId");

        stages.add(stageA);

        decoratedJourneys.add(new JourneyDTO(new LocationDTO(Stations.Cornbrook), new LocationDTO(Stations.ExchangeSquare)
                , stages, TramTime.of(LocalTime.now()), TramTime.of(LocalTime.now()), "summary", "heading", false));

        LocalDate date = LocalDate.now();
        if ((date.getDayOfWeek()==SATURDAY) || (date.getDayOfWeek()==SUNDAY)) {
            date = date.plusDays(3);
        }
        TramServiceDate serviceDate = new TramServiceDate(date);

        List<String> notes = provider.createNotesForJourneys(serviceDate, decoratedJourneys);

        // 1 is for the closure
        assertEquals(1, notes.size());
    }

    @Test
    public void shouldAddNotesForJourneysBasedOnLiveDataIfPresent() {
        List<StageDTO> stages1 = new LinkedList<>();

        StageDTO stageA = createStage(TransportMode.Tram, "platformLocation1", "platformIdA", "Some long message", "displayUnitId1");
        StageDTO stageB = createStage(TransportMode.Tram, "platformLocation2", "platformIdB", "Some long message", "displayUnitId2");
        StageDTO stageC = createStage(TransportMode.Tram, "platformLocation2", "platformIdC", "Some Location Long message", "displayUnitId3");
        StageDTO stageD = createStage(TransportMode.Walk, "platformLocationX", "platformIdD", "Not a tram message", "displayUnitId4");
        StageDTO stageE = createStage(TransportMode.Tram, "platformLocation2", "platformIdE", "Some Location Long message", "displayUnitId5");

        stages1.add(stageA);
        stages1.add(stageB);
        stages1.add(stageC);
        stages1.add(stageD);
        stages1.add(stageE);
        List<StageDTO> stages = stages1;

        decoratedJourneys.add(new JourneyDTO(new LocationDTO(Stations.Cornbrook), new LocationDTO(Stations.ExchangeSquare)
                , stages, TramTime.of(LocalTime.now()), TramTime.of(LocalTime.now()), "summary", "heading", false));

        TramServiceDate serviceDate = new TramServiceDate(LocalDate.now());

        List<String> notes = provider.createNotesForJourneys(serviceDate, decoratedJourneys);

        int expected = 3; // +1 for station closure

        if (serviceDate.isWeekend()) {
            // can't change date as need live data to be available, so update expectations instead
            expected++;
        }

        assertEquals(expected, notes.size());
        assertTrue(notes.toString(), notes.contains("'Some long message' - Metrolink"));
        assertTrue(notes.toString(), notes.contains("'Some Location Long message' - platformLocation2, Metrolink"));
    }

    @Test
    public void shouldAddNotesForStations() {

        List<StationDTO> stations = new LinkedList<>();

        stations.add(createStationDTOwithDepartInfo("first message", "platformLocation1"));
        stations.add(createStationDTOwithDepartInfo("second message", "platformLocation2"));
        stations.add(createStationDTOwithDepartInfo("second message", "platformLocation3"));

        List<String> notes = provider.createNotesForStations(stations);

        assertEquals(3, notes.size());
        assertThat(notes.toString(), notes.contains("'first message' - platformLocation1, Metrolink"));
        assertThat(notes.toString(), notes.contains("'second message' - Metrolink"));

    }

    private StationDTO createStationDTOwithDepartInfo(String message, String platformLocation) {
        Station station = new Station("id", "area", "stopName", new LatLong(0,0), true);
        station.addPlatform(new Platform("platformId1", "name1"));
        station.addPlatform(new Platform("platformId2", "name2"));

        StationDTO stationDTO = new StationDTO(station, ProximityGroup.MY_LOCATION);
        stationDTO.getPlatforms().get(0).setDepartureInfo(createDepartureInfo(platformLocation, message, "displayUnitId"));
        stationDTO.getPlatforms().get(1).setDepartureInfo(createDepartureInfo(platformLocation, message, "displayUnitId"));
        return stationDTO;
    }

    private StageDTO createStage(TransportMode transportMode, String platformLocation, String platformId, String message, String displayUnitId) {
        boolean isWalk = transportMode.equals(TransportMode.Walk);
        PlatformDTO platformDTO = createPlatformDTO(platformLocation, platformId, message, displayUnitId);
        return new StageDTO(new LocationDTO(Stations.Ashton), new LocationDTO(Stations.Victoria),
                new LocationDTO(Stations.PiccadillyGardens), true,
                platformDTO, TramTime.of(LocalTime.now()), TramTime.of(LocalTime.now()), 42,
                "summary", "prompt", "headSign", transportMode, isWalk,
                !isWalk, "displayClass");
    }

    private PlatformDTO createPlatformDTO(String platformLocation, String platformId, String message, String displayUnitId) {
        Platform platform = new Platform(platformId, "platformName");
        PlatformDTO platformDTO = new PlatformDTO(platform);

        platformDTO.setDepartureInfo(createDepartureInfo(platformLocation, message, displayUnitId));
        return platformDTO;
    }

    private StationDepartureInfo createDepartureInfo(String platformLocation, String message, String displayUnitId) {
        return new StationDepartureInfo(displayUnitId,
        "lineName",
        "stationPlatform",
        platformLocation,
        message,
        LocalDateTime.now());
    }

}
