package com.tramchester.unit.domain;

import com.tramchester.domain.Platform;
import com.tramchester.domain.RawVehicleStage;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.*;
import com.tramchester.integration.Stations;
import com.tramchester.livedata.EnrichPlatform;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static junit.framework.TestCase.fail;
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
        List<TransportStage> stages = new LinkedList<>();

        VehicleStageWithTiming stageA = createStage(TransportMode.Tram, "platformIdA");
        VehicleStageWithTiming stageB = createStage(TransportMode.Tram, "platformIdB");
        VehicleStageWithTiming stageC = createStage(TransportMode.Tram, "platformIdC");
        VehicleStageWithTiming stageD = createStage(TransportMode.Walk, "platformIdD");
        VehicleStageWithTiming stageE = createStage(TransportMode.Tram, "platformIdE");

        stages.add(stageA);
        stages.add(stageB);
        stages.add(stageC);
        stages.add(stageD);
        stages.add(stageE);

        Journey journey = new Journey(stages);

        EnrichPlatform enricher = platform -> {
            switch (platform.getId()) {
                case "platformIdA": platform.setDepartureInfo(getStationDepartureInfo(platform, "Some long message"));
                    break;
                case "platformIdB": platform.setDepartureInfo(getStationDepartureInfo(platform, "Some long message"));
                    break;
                case "platformIdC": platform.setDepartureInfo(getStationDepartureInfo(platform, "Some Other Long message"));
                    break;
                case "platformIdD": platform.setDepartureInfo(getStationDepartureInfo(platform, "Not a tram message"));
                    break;
                case "platformIdE": platform.setDepartureInfo(new StationDepartureInfo("303", "lineName",
                        platform.getId(), "some be excluded due to ID", DateTime.now() ));
                    break;
            }
        };
        decoratedJourneys.add(new JourneyDTO(journey, enricher));

        TramServiceDate serviceDate = new TramServiceDate(LocalDate.now());
        List<String> notes = provider.createNotesFor(serviceDate, decoratedJourneys);
        assertEquals(2, notes.size());
        assertTrue(notes.contains("Some long message"));
        assertTrue(notes.contains("Some Other Long message"));
    }

    private StationDepartureInfo getStationDepartureInfo(PlatformDTO platform, String message) {
        return new StationDepartureInfo("displayId", "lineName",
                platform.getId(), message, DateTime.now() );
    }

    private VehicleStageWithTiming createStage(TransportMode transportMode, String platformId) {
        RawVehicleStage rawStage = new RawVehicleStage(Stations.Ashton, "routeName",
                transportMode, "displayClass");
        rawStage.setLastStation(Stations.PiccadillyGardens);
        Platform platform = new Platform(platformId, "platformName");

        rawStage.setPlatform(platform);
        ServiceTime serviceTime = new ServiceTime(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT, "svcId", "headsign", "tripId");
        return new VehicleStageWithTiming(rawStage, serviceTime, TravelAction.Board);

    }

}
