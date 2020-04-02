package com.tramchester.unit.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.*;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.Stations;
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
    private LocalDateTime lastUpdate;

    @Before
    public void beforeEachTestRuns() {
        liveDataRepository = createStrictMock(LiveDataRepository.class);
        provider = new ProvidesNotes(TestConfig.GET(), liveDataRepository);
        journeys = new HashSet<>();
        lastUpdate = TestConfig.LocalNow();
    }

    @Test
    public void shouldAddNotesForClosedStations() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,29));
        List<String> result = provider.createNotesForJourneys(Collections.emptySet(), queryDate);

        assertThat(result, hasItem("St Peters Square is currently closed. "+ProvidesNotes.website));
    }

    @Test
    public void shouldAddNotesForSaturdayJourney() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,29));
        List<String> result = provider.createNotesForJourneys(Collections.emptySet(), queryDate);

        assertThat(result, hasItem(ProvidesNotes.weekend));
    }

    @Test
    public void shouldAddNotesForSundayJourney() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,30));
        List<String> result = provider.createNotesForJourneys(Collections.emptySet(), queryDate);

        assertThat(result, hasItem(ProvidesNotes.weekend));
    }

    @Test
    public void shouldNotShowNotesOnOtherDay() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,31));
        List<String> result = provider.createNotesForJourneys(Collections.emptySet(), queryDate);

        assertThat(result, not(hasItem(ProvidesNotes.weekend)));
    }

    @Test
    public void shouldHaveNoteForChristmasServices() {
        int year = 2018;
        LocalDate date = LocalDate.of(year, 12, 23);

        List<String> result = provider.createNotesForJourneys(Collections.emptySet(), new TramServiceDate(date));
        assertThat(result, not(hasItem(ProvidesNotes.christmas)));

        for(int offset=1; offset<11; offset++) {
            TramServiceDate queryDate = new TramServiceDate(date.plusDays(offset));
            result = provider.createNotesForJourneys(Collections.emptySet(), queryDate);
            assertThat(queryDate.toString(), result, hasItem(ProvidesNotes.christmas));
        }

        date = LocalDate.of(year+1, 1, 3);
        result = provider.createNotesForJourneys(Collections.emptySet(), new TramServiceDate(date));
        assertThat(result, not(hasItem(ProvidesNotes.christmas)));
    }

    @Test
    public void shouldNotAddMessageIfNotMessageForJourney() {
        TransportStage stageA = createStageWithBoardingPlatform("platformId");

        TramTime queryTime = TramTime.of(8,11);
        LocalDate date = TestConfig.LocalNow().toLocalDate();
        if ((date.getDayOfWeek()==SATURDAY) || (date.getDayOfWeek()==SUNDAY)) {
            date = date.plusDays(3);
        }
        TramServiceDate serviceDate = new TramServiceDate(date);

        StationDepartureInfo info = createDepartureInfo(lastUpdate, Stations.Pomona, "<no message>");
        EasyMock.expect(liveDataRepository.departuresFor(stageA.getBoardingPlatform().get(), serviceDate, queryTime)).andReturn(Optional.of(info));

        double cost = 42;
        journeys.add(new Journey(Collections.singletonList(stageA), queryTime));

        replayAll();
        List<String> notes = provider.createNotesForJourneys(journeys, serviceDate);
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

        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime().minusHours(4));
        TramServiceDate serviceDate = new TramServiceDate(lastUpdate.toLocalDate());

        StationDepartureInfo info = createDepartureInfo(lastUpdate, Stations.Pomona, "a message");
        EasyMock.expect(liveDataRepository.departuresFor(stageA.getBoardingPlatform().get(), serviceDate, queryTime)).andReturn(Optional.of(info));

        journeys.add(new Journey(Collections.singletonList(stageA), queryTime));

        replayAll();
        List<String> notes = provider.createNotesForJourneys(journeys, serviceDate);
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

        TramServiceDate queryDate = new TramServiceDate(lastUpdate.toLocalDate().plusDays(2));
        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime());

        StationDepartureInfo info = createDepartureInfo(lastUpdate, Stations.Pomona, "a message");

        EasyMock.expect(liveDataRepository.departuresFor(stageA.getBoardingPlatform().get(), queryDate, queryTime))
                .andReturn(Optional.of(info));

        journeys.add(new Journey(Collections.singletonList(stageA), queryTime));

        replayAll();
        List<String> notes = provider.createNotesForJourneys(journeys, queryDate);
        verifyAll();

        int expected = 1; // 1 is for the closure
        if (queryDate.isWeekend()) {
            expected++;
        }
        if (queryDate.isChristmasPeriod()) {
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
                Stations.Ashton, 7, TramTime.of(8,11), false );
        TransportStage stageE = createStageWithBoardingPlatform("platformId5");

        TramServiceDate serviceDate = new TramServiceDate(lastUpdate.toLocalDate());
        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime());

        StationDepartureInfo infoA = createDepartureInfo(lastUpdate, Stations.Pomona, "Some long message");
        StationDepartureInfo infoB = createDepartureInfo(lastUpdate, Stations.Altrincham, "Some Location Long message");
        StationDepartureInfo infoC = createDepartureInfo(lastUpdate, Stations.Cornbrook, "Some long message");
        StationDepartureInfo infoE = createDepartureInfo(lastUpdate, Stations.MediaCityUK, "Some long message");

        EasyMock.expect(liveDataRepository.departuresFor(stageA.getBoardingPlatform().get(), serviceDate, queryTime)).andReturn(Optional.of(infoA));
        EasyMock.expect(liveDataRepository.departuresFor(stageB.getBoardingPlatform().get(), serviceDate, queryTime)).andReturn(Optional.of(infoB));
        EasyMock.expect(liveDataRepository.departuresFor(stageC.getBoardingPlatform().get(), serviceDate, queryTime)).andReturn(Optional.of(infoC));
        EasyMock.expect(liveDataRepository.departuresFor(stageE.getBoardingPlatform().get(), serviceDate, queryTime)).andReturn(Optional.of(infoE));

        List<TransportStage> stages = Arrays.asList(stageA, stageB, stageC, stageD, stageE);

        journeys.add(new Journey(stages, queryTime));

        replayAll();
        List<String> notes = provider.createNotesForJourneys(journeys, serviceDate);
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

        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016, 10, 25));
        TramTime queryTime = TramTime.of(lastUpdate);
        EasyMock.expect(liveDataRepository.departuresFor(Stations.Pomona, queryDate, queryTime)).
                andReturn(Collections.singletonList(createDepartureInfo(lastUpdate, Stations.Pomona, "second message")));
        EasyMock.expect(liveDataRepository.departuresFor(Stations.VeloPark, queryDate, queryTime)).
                andReturn(Collections.singletonList(createDepartureInfo(lastUpdate, Stations.VeloPark, "first message")));
        EasyMock.expect(liveDataRepository.departuresFor(Stations.Cornbrook, queryDate, queryTime)).
                andReturn(Collections.singletonList(createDepartureInfo(lastUpdate, Stations.Cornbrook, "second message")));

        replayAll();

        List<String> notes = provider.createNotesForStations(stations, queryDate, queryTime);
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
        Service service = new Service("serviceId", TestConfig.getTestRoute());
        Trip trip = new Trip("tripId", "headSign", service, TestConfig.getTestRoute());
        VehicleStage vehicleStage = new VehicleStage(Stations.Ashton, TestConfig.getTestRoute(), TransportMode.Tram, "displayClass",
                trip, departTime, Stations.PiccadillyGardens, 12);
        Platform platform = new Platform(platformId, "platformName");
        vehicleStage.setPlatform(platform);
        return vehicleStage;
    }


}
