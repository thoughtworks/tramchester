package com.tramchester.unit.domain;

import com.tramchester.domain.Platform;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.integration.Stations;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ProvidesNotesTest {
    private ProvidesNotes provider;
    private SortedSet<JourneyDTO> decoratedJourneys;

    @Before
    public void beforeEachTestRuns() {
        decoratedJourneys = new TreeSet<>();
        provider = new ProvidesNotes();
    }

    @Test
    public void shouldAddNotesForSaturdayJourney() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.parse("2016-10-29"));
        List<String> result = provider.createNotesFor(queryDate, decoratedJourneys);

        assertThat(result, hasItem(ProvidesNotes.weekend));
    }

    @Test
    public void shouldAddNotesForSundayJourney() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.parse("2016-10-30"));
        List<String> result = provider.createNotesFor(queryDate, decoratedJourneys);

        assertThat(result, hasItem(ProvidesNotes.weekend));
    }

    @Test
    public void shouldNotShowNotesOnOtherDay() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.parse("2016-10-31"));
        List<String> result = provider.createNotesFor(queryDate, decoratedJourneys);

        assertThat(result, not(hasItem(ProvidesNotes.weekend)));
    }

    @Test
    public void shouldHaveNoteForChristmasServices2016() {
        LocalDate date = new LocalDate(2016, 12, 23);

        List<String> result = provider.createNotesFor(new TramServiceDate(date), decoratedJourneys);
        assertThat(result, not(hasItem(ProvidesNotes.christmas)));

        for(int offset=1; offset<11; offset++) {
            TramServiceDate queryDate = new TramServiceDate(date.plusDays(offset));
            result = provider.createNotesFor(queryDate, decoratedJourneys);
            assertThat(queryDate.toString(), result, hasItem(ProvidesNotes.christmas));
        }

        date = new LocalDate(2017, 1, 3);
        result = provider.createNotesFor(new TramServiceDate(date), decoratedJourneys);
        assertThat(result, not(hasItem(ProvidesNotes.christmas)));
    }

    @Test
    public void shouldAddNotesBasedOnLiveDataIfPresent() {
        List<StageDTO> stages = new LinkedList<>();

        StageDTO stageA = createStage(TransportMode.Tram, "platformIdA", "Some long message", "displayUnitId");
        StageDTO stageB = createStage(TransportMode.Tram, "platformIdB", "Some long message", "displayUnitId");
        StageDTO stageC = createStage(TransportMode.Tram, "platformIdC", "Some Other Long message", "displayUnitId");
        StageDTO stageD = createStage(TransportMode.Walk, "platformIdD", "Not a tram message", "displayUnitId");
        StageDTO stageE = createStage(TransportMode.Tram, "platformIdE", "excluded due to display ID", "303");

        stages.add(stageA);
        stages.add(stageB);
        stages.add(stageC);
        stages.add(stageD);
        stages.add(stageE);

        decoratedJourneys.add(new JourneyDTO(new LocationDTO(Stations.Cornbrook), new LocationDTO(Stations.ExchangeSquare)
                , stages, LocalTime.now(), LocalTime.now(), "summary", "heading"));

        TramServiceDate serviceDate = new TramServiceDate(LocalDate.now());

        List<String> notes = provider.createNotesFor(serviceDate, decoratedJourneys);

        assertEquals(2, notes.size());
        assertTrue(notes.contains("'Some long message' - Metrolink"));
        assertTrue(notes.contains("'Some Other Long message' - Metrolink"));
    }

    private StationDepartureInfo getStationDepartureInfo(PlatformDTO platform, String message) {
        return new StationDepartureInfo("displayId", "lineName",
                platform.getId(), message, DateTime.now() );
    }

    private StageDTO createStage(TransportMode transportMode, String platformId, String message, String displayUnitId) {
        boolean isWalk = transportMode.equals(TransportMode.Walk);
        Platform platform = new Platform(platformId, "platformName");
        PlatformDTO platformDTO = new PlatformDTO(platform);

        platformDTO.setDepartureInfo(createDepartureInfo(message, displayUnitId));
        return new StageDTO(new LocationDTO(Stations.Ashton), new LocationDTO(Stations.Victoria),
                new LocationDTO(Stations.PiccadillyGardens), true,
                platformDTO, LocalTime.now(), LocalTime.now(), 42,
                "summary", "prompt", "headSign", transportMode, isWalk,
                !isWalk, "displayClass");
    }

    private StationDepartureInfo createDepartureInfo(String message, String displayUnitId) {
        return new StationDepartureInfo(displayUnitId,
        "lineName",
        "stationPlatform",
        message,
        DateTime.now());
    }

}
