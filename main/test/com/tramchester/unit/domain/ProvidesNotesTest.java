package com.tramchester.unit.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.TestConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.*;
import com.tramchester.integration.Stations;
import com.tramchester.repository.LiveDataRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ProvidesNotesTest extends EasyMockSupport {
    private ProvidesNotes provider;
    private Set<Journey> journeys;
    private LiveDataRepository liveDataRepository;

    @Before
    public void beforeEachTestRuns() {
        liveDataRepository = createStrictMock(LiveDataRepository.class);
        provider = new ProvidesNotes(TestConfig.GET(), liveDataRepository);
        journeys = new HashSet<>();
    }

    @Test
    public void shouldAddNotesForClosedStations() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,29));
        List<String> result = provider.createNotesForJourneys(queryDate, Collections.emptySet());

        assertThat(result, hasItem("St Peters Square is currently closed. "+ProvidesNotes.website));
    }

    @Test
    public void shouldAddNotesForSaturdayJourney() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,29));
        List<String> result = provider.createNotesForJourneys(queryDate, Collections.emptySet());

        assertThat(result, hasItem(ProvidesNotes.weekend));
    }

    @Test
    public void shouldAddNotesForSundayJourney() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,30));
        List<String> result = provider.createNotesForJourneys(queryDate, Collections.emptySet());

        assertThat(result, hasItem(ProvidesNotes.weekend));
    }

    @Test
    public void shouldNotShowNotesOnOtherDay() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,31));
        List<String> result = provider.createNotesForJourneys(queryDate, Collections.emptySet());

        assertThat(result, not(hasItem(ProvidesNotes.weekend)));
    }

    @Test
    public void shouldHaveNoteForChristmasServices() {
        int year = 2018;
        LocalDate date = LocalDate.of(year, 12, 23);

        List<String> result = provider.createNotesForJourneys(new TramServiceDate(date), Collections.emptySet());
        assertThat(result, not(hasItem(ProvidesNotes.christmas)));

        for(int offset=1; offset<11; offset++) {
            TramServiceDate queryDate = new TramServiceDate(date.plusDays(offset));
            result = provider.createNotesForJourneys(queryDate, Collections.emptySet());
            assertThat(queryDate.toString(), result, hasItem(ProvidesNotes.christmas));
        }

        date = LocalDate.of(year+1, 1, 3);
        result = provider.createNotesForJourneys(new TramServiceDate(date), Collections.emptySet());
        assertThat(result, not(hasItem(ProvidesNotes.christmas)));
    }

    @Test
    public void shouldNotAddMessageIfNotMessageForJourney() {
        TransportStage stageA = createStageWithBoardingPlatform("platformId");

        LocalDateTime lastUpdate = LocalDateTime.now();
        StationDepartureInfo info = createDepartureInfo(lastUpdate, Stations.Pomona, "<no message>");
        EasyMock.expect(liveDataRepository.departuresFor(stageA.getBoardingPlatform().get())).andReturn(Optional.of(info));

        TramTime queryTime = TramTime.of(8,11);
        double cost = 42;
        journeys.add(new Journey(Collections.singletonList(stageA), queryTime, cost));

        LocalDate date = LocalDate.now();
        if ((date.getDayOfWeek()==SATURDAY) || (date.getDayOfWeek()==SUNDAY)) {
            date = date.plusDays(3);
        }
        TramServiceDate serviceDate = new TramServiceDate(date);

        replayAll();
        List<String> notes = provider.createNotesForJourneys(serviceDate, journeys);
        verifyAll();

        // 1 is for the closure
        int expected = 1;
        if (serviceDate.isChristmasPeriod()) {
            expected++;
        }
        assertEquals(expected, notes.size());
    }

    @Test
    public void shouldNotAddMessageIfNotMessageIfNotTimelTime() {
        TransportStage stageA = createStageWithBoardingPlatform("platformId");

        LocalDateTime lastUpdate = LocalDateTime.now();

        StationDepartureInfo info = createDepartureInfo(lastUpdate, Stations.Pomona, "a message");
        EasyMock.expect(liveDataRepository.departuresFor(stageA.getBoardingPlatform().get())).andReturn(Optional.of(info));

        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime().minusHours(4));
        journeys.add(new Journey(Collections.singletonList(stageA), queryTime, 42));

        TramServiceDate serviceDate = new TramServiceDate(lastUpdate.toLocalDate());

        replayAll();
        List<String> notes = provider.createNotesForJourneys(serviceDate, journeys);
        verifyAll();

        int expected = 1; // 1 is for the closure
        if (serviceDate.isWeekend()) {
            expected++;
        }
        if (serviceDate.isChristmasPeriod()) {
            expected++;
        }
        assertEquals(expected, notes.size());
    }

    @Test
    public void shouldNotAddMessageIfNotMessageIfNotTimelyDate() {
        TransportStage stageA = createStageWithBoardingPlatform("platformId");

        LocalDateTime lastUpdate = LocalDateTime.now();

        StationDepartureInfo info = createDepartureInfo(lastUpdate, Stations.Pomona, "a message");
        EasyMock.expect(liveDataRepository.departuresFor(stageA.getBoardingPlatform().get())).andReturn(Optional.of(info));

        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime());
        journeys.add(new Journey(Collections.singletonList(stageA), queryTime, 42));

        TramServiceDate serviceDate = new TramServiceDate(lastUpdate.toLocalDate().plusDays(2));

        replayAll();
        List<String> notes = provider.createNotesForJourneys(serviceDate, journeys);
        verifyAll();

        int expected = 1; // 1 is for the closure
        if (serviceDate.isWeekend()) {
            expected++;
        }
        if (serviceDate.isChristmasPeriod()) {
            expected++;
        }
        assertEquals(expected, notes.size());
    }

    @Test
    public void shouldAddNotesForJourneysBasedOnLiveDataIfPresent() {

        TransportStage stageA = createStageWithBoardingPlatform("platformId1");
        TransportStage stageB = createStageWithBoardingPlatform("platformId2");
        TransportStage stageC = createStageWithBoardingPlatform("platformId3");
        TransportStage stageD = new WalkingStage(MyLocation.create(new ObjectMapper(), TestConfig.nearAltrincham),
                Stations.Ashton, 7, TramTime.of(8,11) );
        TransportStage stageE = createStageWithBoardingPlatform("platformId5");

        LocalDateTime lastUpdate = LocalDateTime.now();

        StationDepartureInfo infoA = createDepartureInfo(lastUpdate, Stations.Pomona, "Some long message");
        StationDepartureInfo infoB = createDepartureInfo(lastUpdate, Stations.Altrincham, "Some Location Long message");
        StationDepartureInfo infoC = createDepartureInfo(lastUpdate, Stations.Cornbrook, "Some long message");
        StationDepartureInfo infoE = createDepartureInfo(lastUpdate, Stations.MediaCityUK, "Some long message");

        EasyMock.expect(liveDataRepository.departuresFor(stageA.getBoardingPlatform().get())).andReturn(Optional.of(infoA));
        EasyMock.expect(liveDataRepository.departuresFor(stageB.getBoardingPlatform().get())).andReturn(Optional.of(infoB));
        EasyMock.expect(liveDataRepository.departuresFor(stageC.getBoardingPlatform().get())).andReturn(Optional.of(infoC));
        EasyMock.expect(liveDataRepository.departuresFor(stageE.getBoardingPlatform().get())).andReturn(Optional.of(infoE));

        List<TransportStage> stages = Arrays.asList(stageA, stageB, stageC, stageD, stageE);

        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime());
        journeys.add(new Journey(stages, queryTime, 89));
        TramServiceDate serviceDate = new TramServiceDate(LocalDate.now());

        replayAll();
        List<String> notes = provider.createNotesForJourneys(serviceDate, journeys);
        verifyAll();

        int expected = 3; // +1 for station closure

        if (serviceDate.isWeekend()) {
            // can't change date as need live data to be available, so update expectations instead
            expected++;
        }

        if (serviceDate.isChristmasPeriod()) {
            expected++;
        }

        assertEquals(expected, notes.size());
        assertTrue(notes.toString(), notes.contains("'Some long message' - Metrolink"));
        assertTrue(notes.toString(), notes.contains("'Some Location Long message' - Altrincham, Metrolink"));
    }

    @Test
    public void shouldAddNotesForStations() {

        List<Station> stations = Arrays.asList(Stations.Pomona, Stations.VeloPark, Stations.Cornbrook);

        LocalDateTime time = LocalDateTime.now();
        EasyMock.expect(liveDataRepository.departuresFor(Stations.Pomona)).
                andReturn(Collections.singletonList(createDepartureInfo(time, Stations.Pomona, "second message")));
        EasyMock.expect(liveDataRepository.departuresFor(Stations.VeloPark)).
                andReturn(Collections.singletonList(createDepartureInfo(time, Stations.VeloPark, "first message")));
        EasyMock.expect(liveDataRepository.departuresFor(Stations.Cornbrook)).
                andReturn(Collections.singletonList(createDepartureInfo(time, Stations.Cornbrook, "second message")));

        replayAll();
        List<String> notes = provider.createNotesForStations(stations, new TramServiceDate(LocalDate.of(2016,10,25)));
        verifyAll();

        assertEquals(3, notes.size());
        assertThat(notes.toString(), notes.contains("'first message' - Velopark, Metrolink"));
        assertThat(notes.toString(), notes.contains("'second message' - Metrolink"));

    }

    private StationDepartureInfo createDepartureInfo(LocalDateTime time, Station station, String message) {
        return new StationDepartureInfo("displayId", "lineName", StationDepartureInfo.Direction.Incoming,
                "platform", station, message, time);
    }

    private TransportStage createStageWithBoardingPlatform(String platformId) {
        TramTime departTime = TramTime.of(11,22);
        Trip trip = new Trip("tripId", "headSign", "serviceId", "routeId");
        VehicleStage vehicleStage = new VehicleStage(Stations.Ashton, "routeName", TransportMode.Tram, "displayClass",
                trip, departTime, Stations.PiccadillyGardens, 12);
        Platform platform = new Platform(platformId, "platformName");
        vehicleStage.setPlatform(platform);
        return vehicleStage;
    }


}
