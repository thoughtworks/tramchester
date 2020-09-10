package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.presentation.StationNote;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.LiveDataRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.tramchester.domain.presentation.Note.NoteType.Live;
import static com.tramchester.testSupport.TramStations.*;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

class ProvidesNotesTest extends EasyMockSupport {
    private ProvidesNotes provider;
    private LiveDataRepository liveDataRepository;
    private LocalDateTime lastUpdate;

    @BeforeEach
    void beforeEachTestRuns() {
        liveDataRepository = createStrictMock(LiveDataRepository.class);
        provider = new ProvidesNotes(liveDataRepository);
        lastUpdate = TestEnv.LocalNow();
    }

    @Test
    void shouldAddNotesForSaturdayJourney() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,29));

        replayAll();
        List<Note> result = provider.createNotesForJourney(new Journey(Collections.emptyList(), TramTime.of(11,45), Collections.emptyList()), queryDate);
        verifyAll();

        assertThat(result, hasItem(new Note(ProvidesNotes.weekend, Note.NoteType.Weekend)));
    }

    @Test
    void shouldAddNotesForSundayJourney() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,30));

        replayAll();
        List<Note> result = provider.createNotesForJourney(new Journey(Collections.emptyList(), TramTime.of(11,45), Collections.emptyList()), queryDate);
        verifyAll();

        assertThat(result, hasItem(new Note(ProvidesNotes.weekend, Note.NoteType.Weekend)));
    }

    @Test
    void shouldNotShowNotesOnOtherDay() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,31));

        replayAll();
        List<Note> result = provider.createNotesForJourney(new Journey(Collections.emptyList(), TramTime.of(11,45), Collections.emptyList()), queryDate);
        verifyAll();

        assertThat(result, not(hasItem(new Note(ProvidesNotes.weekend, Note.NoteType.Weekend))));
    }

    @Test
    void shouldHaveNoteForChristmasServices() {
        int year = 2018;
        LocalDate date = LocalDate.of(year, 12, 23);
        Note christmasNote = new Note(ProvidesNotes.christmas, Note.NoteType.Christmas);

        Journey journey = new Journey(Collections.emptyList(), TramTime.of(11, 45), Collections.emptyList());

        replayAll();

        List<Note> result = provider.createNotesForJourney(journey, new TramServiceDate(date));
        assertThat(result, not(hasItem(christmasNote)));

        for(int offset=1; offset<11; offset++) {
            TramServiceDate queryDate = new TramServiceDate(date.plusDays(offset));
            result = provider.createNotesForJourney(journey, queryDate);
            assertThat(queryDate.toString(), result, hasItem(christmasNote));
        }

        date = LocalDate.of(year+1, 1, 3);

        result = provider.createNotesForJourney(journey, new TramServiceDate(date));

        verifyAll();

        assertThat(result, not(hasItem(christmasNote)));
    }

    @Test
    void shouldNotAddMessageIfNotMessageForJourney() {
        VehicleStage stageA = createStageWithBoardingPlatform("platformId");

        TramTime queryTime = TramTime.of(8,11);
        LocalDate date = TestEnv.LocalNow().toLocalDate();
        if ((date.getDayOfWeek()==SATURDAY) || (date.getDayOfWeek()==SUNDAY)) {
            date = date.plusDays(3);
        }
        TramServiceDate serviceDate = new TramServiceDate(date);

        StationDepartureInfo info = createDepartureInfo(lastUpdate, of(Pomona), "<no message>");
        EasyMock.expect(liveDataRepository.departuresFor(stageA.getBoardingPlatform().get().getId(), serviceDate, queryTime)).
                andReturn(Optional.of(info));

        Journey journey = new Journey(Collections.singletonList(stageA), queryTime, Collections.emptyList());

        replayAll();
        List<Note> notes = provider.createNotesForJourney(journey, serviceDate);
        verifyAll();

        int expected = 0;
        if (serviceDate.isChristmasPeriod()) {
            expected++;
        }
        Assertions.assertEquals(expected, notes.size());
    }

    @Test
    void shouldNotAddMessageIfNotMessageIfNotTimelTime() {
        VehicleStage stageA = createStageWithBoardingPlatform("platformId");

        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime().minusHours(4));
        TramServiceDate serviceDate = new TramServiceDate(lastUpdate.toLocalDate());

        StationDepartureInfo info = createDepartureInfo(lastUpdate, of(Pomona), "a message");
        EasyMock.expect(liveDataRepository.departuresFor(stageA.getBoardingPlatform().get().getId(), serviceDate, queryTime)).
                andReturn(Optional.of(info));

        Journey journey = new Journey(Collections.singletonList(stageA), queryTime, Collections.emptyList());

        replayAll();
        List<Note> notes = provider.createNotesForJourney(journey, serviceDate);
        verifyAll();

        int expected = 0;
        if (serviceDate.isWeekend()) {
            expected++;
        }
        if (serviceDate.isChristmasPeriod()) {
            expected++;
        }
        Assertions.assertEquals(expected, notes.size());
    }

    @Test
    void shouldNotAddMessageIfNotMessageIfNotTimelyDate() {
        VehicleStage stageA = createStageWithBoardingPlatform("platformId");

        TramServiceDate queryDate = new TramServiceDate(lastUpdate.toLocalDate().plusDays(2));
        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime());

        StationDepartureInfo info = createDepartureInfo(lastUpdate, of(Pomona), "a message");

        EasyMock.expect(liveDataRepository.departuresFor(stageA.getBoardingPlatform().get().getId(), queryDate, queryTime))
                .andReturn(Optional.of(info));

        Journey journey = new Journey(Collections.singletonList(stageA), queryTime, Collections.emptyList());

        replayAll();
        List<Note> notes = provider.createNotesForJourney(journey, queryDate);
        verifyAll();

        int expected = 0;
        if (queryDate.isWeekend()) {
            expected++;
        }
        if (queryDate.isChristmasPeriod()) {
            expected++;
        }
        Assertions.assertEquals(expected, notes.size());
    }

    @Test
    void shouldAddNotesForJourneysBasedOnLiveDataIfPresent() {
        VehicleStage stageA = createStageWithBoardingPlatform("platformId1");
        VehicleStage stageB = createStageWithBoardingPlatform("platformId2");
        VehicleStage stageC = createStageWithBoardingPlatform("platformId3");
        WalkingToStationStage stageD = new WalkingToStationStage(new MyLocation("Altrincham",TestEnv.nearAltrincham),
                TramStations.of(Ashton), 7, TramTime.of(8,11));
        VehicleStage stageE = createStageWithBoardingPlatform("platformId5");

        TramServiceDate serviceDate = new TramServiceDate(lastUpdate.toLocalDate());
        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime());

        StationDepartureInfo infoA = createDepartureInfo(lastUpdate, TramStations.of(Pomona), "Some long message");
        StationDepartureInfo infoB = createDepartureInfo(lastUpdate, TramStations.of(Altrincham), "Some Location Long message");
        StationDepartureInfo infoC = createDepartureInfo(lastUpdate, TramStations.of(Cornbrook), "Some long message");
        StationDepartureInfo infoE = createDepartureInfo(lastUpdate, TramStations.of(MediaCityUK), "Some long message");

        EasyMock.expect(liveDataRepository.departuresFor(stageE.getBoardingPlatform().get().getId(), serviceDate, queryTime)).andReturn(Optional.of(infoE));
        EasyMock.expect(liveDataRepository.departuresFor(stageB.getBoardingPlatform().get().getId(), serviceDate, queryTime)).andReturn(Optional.of(infoB));
        EasyMock.expect(liveDataRepository.departuresFor(stageC.getBoardingPlatform().get().getId(), serviceDate, queryTime)).andReturn(Optional.of(infoC));
        EasyMock.expect(liveDataRepository.departuresFor(stageA.getBoardingPlatform().get().getId(), serviceDate, queryTime)).andReturn(Optional.of(infoA));

        List<TransportStage<?,?>> stages = Arrays.asList(stageA, stageB, stageC, stageD, stageE);

        Journey journey = new Journey(stages, queryTime, Collections.emptyList());

        replayAll();
        List<Note> notes = provider.createNotesForJourney(journey, serviceDate);
        verifyAll();

        int expected = 4;

        if (serviceDate.isWeekend()) {
            // can't change date as need live data to be available, so update expectations instead
            expected++;
        }

        if (serviceDate.isChristmasPeriod()) {
            expected++;
        }

        Assertions.assertEquals(expected, notes.size());
        Assertions.assertTrue(notes.contains(new StationNote(Live,"Some long message", of(Pomona))), notes.toString());
        Assertions.assertTrue(notes.contains(new StationNote(Live,"Some long message", of(Cornbrook))), notes.toString());
        Assertions.assertTrue(notes.contains(new StationNote(Live,"Some long message", of(MediaCityUK))), notes.toString());
        Assertions.assertTrue(notes.contains(new StationNote(Live,"Some Location Long message", of(Altrincham))), notes.toString());
    }

    @Test
    void shouldAddNotesForStations() {

        List<Station> stations = Arrays.asList(of(Pomona), of(VeloPark), of(Cornbrook));

        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016, 10, 25));
        TramTime queryTime = TramTime.of(lastUpdate);
        EasyMock.expect(liveDataRepository.departuresFor(of(Pomona), queryDate, queryTime)).
                andReturn(Collections.singletonList(createDepartureInfo(lastUpdate, of(Pomona), "second message")));
        EasyMock.expect(liveDataRepository.departuresFor(of(VeloPark), queryDate, queryTime)).
                andReturn(Collections.singletonList(createDepartureInfo(lastUpdate, of(VeloPark), "first message")));
        EasyMock.expect(liveDataRepository.departuresFor(of(Cornbrook), queryDate, queryTime)).
                andReturn(Collections.singletonList(createDepartureInfo(lastUpdate, of(Cornbrook), "second message")));

        replayAll();

        List<Note> notes = provider.createNotesForStations(stations, queryDate, queryTime);
        verifyAll();

        Assertions.assertEquals(3, notes.size());
        assertThat(notes.toString(), notes.contains(new StationNote(Live,"first message", of(VeloPark))));
        assertThat(notes.toString(), notes.contains(new StationNote(Live,"second message", of(Cornbrook))));
        assertThat(notes.toString(), notes.contains(new StationNote(Live,"second message", of(Pomona))));
    }

    private StationDepartureInfo createDepartureInfo(LocalDateTime time, Station station, String message) {
        return new StationDepartureInfo("displayId", "lineName", StationDepartureInfo.Direction.Incoming,
                "platform", station, message, time);
    }

    private VehicleStage createStageWithBoardingPlatform(String platformId) {
        TramTime departTime = TramTime.of(11,22);
        Service service = new Service("serviceId", TestEnv.getTestRoute());
        Trip trip = new Trip("tripId", "headSign", service, TestEnv.getTestRoute());
        VehicleStage vehicleStage = new VehicleStage(of(Ashton), TestEnv.getTestRoute(), TransportMode.Tram,
                trip, departTime, of(PiccadillyGardens), 12);
        Platform platform = new Platform(platformId, "platformName");
        vehicleStage.setPlatform(platform);
        return vehicleStage;
    }

}
