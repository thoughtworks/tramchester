package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.liveUpdates.PlatformMessage;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.*;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.VehicleStage;
import com.tramchester.domain.transportStages.WalkingToStationStage;
import com.tramchester.repository.PlatformMessageSource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.tramchester.domain.presentation.Note.NoteType.Live;
import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProvidesNotesTest extends EasyMockSupport {
    private ProvidesNotes providesNotes;
    private PlatformMessageSource platformMessageSource;
    private LocalDateTime lastUpdate;
    private final int requestedNumberChanges = 2;

    @BeforeEach
    void beforeEachTestRuns() {
        platformMessageSource = createStrictMock(PlatformMessageSource.class);
        providesNotes = new ProvidesNotes(platformMessageSource);
        lastUpdate = TestEnv.LocalNow();
    }

    @Test
    void shouldAddNotesForSaturdayJourney() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,29));

        Journey journey = createMock(Journey.class);
        EasyMock.expect(journey.getTransportModes()).andReturn(Collections.singleton(Tram));
        EasyMock.expect(journey.getCallingPlatformIds()).andReturn(IdSet.emptySet());

        replayAll();
        List<Note> result = providesNotes.createNotesForJourney(journey, queryDate);
        verifyAll();

        assertThat(result, hasItem(new Note(ProvidesNotes.weekend, Note.NoteType.Weekend)));
    }

    @Test
    void shouldStillAddNotesForSaturdayJourneySourceDisabled() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(false);

        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,29));

        Journey journey = createMock(Journey.class);
        EasyMock.expect(journey.getTransportModes()).andReturn(Collections.singleton(Tram));

        replayAll();
        List<Note> result = providesNotes.createNotesForJourney(journey, queryDate);
        verifyAll();

        assertThat(result, hasItem(new Note(ProvidesNotes.weekend, Note.NoteType.Weekend)));
    }

    @Test
    void shouldNotAddNotesForWeekendWhenNoTramInvolved() {

        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,29));

        Journey journey = createMock(Journey.class);
        Set<TransportMode> modes = new HashSet<>(Arrays.asList(Train, Bus, Walk));
        EasyMock.expect(journey.getTransportModes()).andReturn(modes);

        replayAll();
        List<Note> result = providesNotes.createNotesForJourney(journey, queryDate);
        verifyAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldAddNotesForSundayJourney() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);
        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,30));

        Journey journey = createMock(Journey.class);
        EasyMock.expect(journey.getTransportModes()).andReturn(Collections.singleton(Tram));
        EasyMock.expect(journey.getCallingPlatformIds()).andReturn(IdSet.emptySet());

        replayAll();
        List<Note> result = providesNotes.createNotesForJourney(journey, queryDate);
        verifyAll();

        assertThat(result, hasItem(new Note(ProvidesNotes.weekend, Note.NoteType.Weekend)));
    }

    @Test
    void shouldNotShowNotesOnOtherDay() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,31));

        Journey journey = createMock(Journey.class);
        EasyMock.expect(journey.getTransportModes()).andReturn(Collections.singleton(Tram));
        EasyMock.expect(journey.getCallingPlatformIds()).andReturn(IdSet.emptySet());

        replayAll();
        List<Note> result = providesNotes.createNotesForJourney(journey, queryDate);
        verifyAll();

        assertThat(result, not(hasItem(new Note(ProvidesNotes.weekend, Note.NoteType.Weekend))));
    }

    @Test
    void shouldHandleDisabled() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(false);

        TramServiceDate queryDate = new TramServiceDate(LocalDate.of(2016,10,31));

        Journey journey = createMock(Journey.class);
        EasyMock.expect(journey.getTransportModes()).andReturn(Collections.singleton(Tram));

        replayAll();
        List<Note> result = providesNotes.createNotesForJourney(journey, queryDate);
        verifyAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHaveNoteForChristmasTramServices() {
        EasyMock.expect(platformMessageSource.isEnabled()).andStubReturn(true);

        int year = 2020;
        LocalDate date = LocalDate.of(year, 12, 23);
        Note christmasNote = new Note(ProvidesNotes.christmas2020, Note.NoteType.Christmas);

        Journey journey = createMock(Journey.class);
        EasyMock.expect(journey.getTransportModes()).andStubReturn(Collections.singleton(Tram));
        EasyMock.expect(journey.getCallingPlatformIds()).andStubReturn(IdSet.emptySet());

        replayAll();

        List<Note> result = providesNotes.createNotesForJourney(journey, new TramServiceDate(date));
        assertThat(result, not(hasItem(christmasNote)));

        for(int offset=1; offset<11; offset++) {
            TramServiceDate queryDate = new TramServiceDate(date.plusDays(offset));
            result = providesNotes.createNotesForJourney(journey, queryDate);
            assertThat(queryDate.toString(), result, hasItem(christmasNote));
        }

        date = LocalDate.of(year+1, 1, 3);

        result = providesNotes.createNotesForJourney(journey, new TramServiceDate(date));

        verifyAll();

        assertThat(result, not(hasItem(christmasNote)));
    }

    @Test
    void shouldHaveNoNoteForChristmasServicesIfNotTram() {
        EasyMock.expect(platformMessageSource.isEnabled()).andStubReturn(true);

        int year = 2020;
        LocalDate date = LocalDate.of(year, 12, 23);
        Note christmasNote = new Note(ProvidesNotes.christmas, Note.NoteType.Christmas);

        Journey journey = createMock(Journey.class);
        Set<TransportMode> modes = new HashSet<>(Arrays.asList(Train, Bus, Walk));

        EasyMock.expect(journey.getTransportModes()).andStubReturn(modes);

        replayAll();

        List<Note> result = providesNotes.createNotesForJourney(journey, new TramServiceDate(date));
        assertTrue(result.isEmpty());

        for(int offset=1; offset<11; offset++) {
            TramServiceDate queryDate = new TramServiceDate(date.plusDays(offset));
            result = providesNotes.createNotesForJourney(journey, queryDate);
            assertTrue(result.isEmpty());
        }

        date = LocalDate.of(year+1, 1, 3);

        result = providesNotes.createNotesForJourney(journey, new TramServiceDate(date));

        verifyAll();

        assertThat(result, not(hasItem(christmasNote)));
    }

    @Test
    void shouldNotAddMessageIfNotMessageForJourney() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        VehicleStage stageA = createStageWithBoardingPlatform("platformId", TestEnv.nearPiccGardens);

        TramTime queryTime = TramTime.of(8,11);
        LocalDate date = TestEnv.LocalNow().toLocalDate();
        if ((date.getDayOfWeek()==SATURDAY) || (date.getDayOfWeek()==SUNDAY)) {
            date = date.plusDays(3);
        }
        TramServiceDate serviceDate = new TramServiceDate(date);

        PlatformMessage info = createPlatformMessage(lastUpdate, of(Pomona), "<no message>");
        EasyMock.expect(platformMessageSource.messagesFor(stageA.getBoardingPlatform().getId(), date, queryTime)).
                andReturn(Optional.of(info));

        Journey journey = new Journey(queryTime.plusMinutes(5), queryTime, queryTime.plusMinutes(10), Collections.singletonList(stageA), Collections.emptyList(),
                requestedNumberChanges);

        replayAll();
        List<Note> notes = providesNotes.createNotesForJourney(journey, serviceDate);
        verifyAll();

        int expected = 0;
        if (serviceDate.isChristmasPeriod()) {
            expected++;
        }
        Assertions.assertEquals(expected, notes.size());
    }

    @Test
    void shouldNotAddMessageIfNotMessageIfNotTimelTime() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        VehicleStage stageA = createStageWithBoardingPlatform("platformId", TestEnv.nearPiccGardens);

        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime().minusHours(4));
        TramServiceDate serviceDate = new TramServiceDate(lastUpdate.toLocalDate());

        PlatformMessage info = createPlatformMessage(lastUpdate, of(Pomona), "a message");
        EasyMock.expect(platformMessageSource.messagesFor(stageA.getBoardingPlatform().getId(), lastUpdate.toLocalDate(), queryTime)).
                andReturn(Optional.of(info));

        Journey journey = new Journey(queryTime.plusMinutes(5), queryTime, queryTime.plusMinutes(10), Collections.singletonList(stageA), Collections.emptyList(),
                requestedNumberChanges);

        replayAll();
        List<Note> notes = providesNotes.createNotesForJourney(journey, serviceDate);
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
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        VehicleStage stageA = createStageWithBoardingPlatform("platformId", TestEnv.nearPiccGardens);

        LocalDate localDate = lastUpdate.toLocalDate().plusDays(2);
        TramServiceDate queryDate = new TramServiceDate(localDate);
        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime());

        PlatformMessage info = createPlatformMessage(lastUpdate, of(Pomona), "a message");

        EasyMock.expect(platformMessageSource.messagesFor(stageA.getBoardingPlatform().getId(), localDate, queryTime))
                .andReturn(Optional.of(info));

        Journey journey = new Journey(queryTime.plusMinutes(5), queryTime, queryTime.plusMinutes(10), Collections.singletonList(stageA), Collections.emptyList(),
                requestedNumberChanges);

        replayAll();
        List<Note> notes = providesNotes.createNotesForJourney(journey, queryDate);
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
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        VehicleStage stageA = createStageWithBoardingPlatform("platformId1", Bury.getLatLong());
        VehicleStage stageB = createStageWithBoardingPlatform("platformId2", Cornbrook.getLatLong());
        VehicleStage stageC = createStageWithBoardingPlatform("platformId3", NavigationRoad.getLatLong());
        WalkingToStationStage stageD = new WalkingToStationStage(new MyLocation("Altrincham", TestEnv.nearAltrincham),
                TramStations.of(Ashton), 7, TramTime.of(8,11));
        VehicleStage stageE = createStageWithBoardingPlatform("platformId5", Altrincham.getLatLong());

        TramServiceDate serviceDate = new TramServiceDate(lastUpdate.toLocalDate());
        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime());

        PlatformMessage infoA = createPlatformMessage(lastUpdate, TramStations.of(Pomona), "Some long message");
        PlatformMessage infoB = createPlatformMessage(lastUpdate, TramStations.of(Altrincham), "Some Location Long message");
        PlatformMessage infoC = createPlatformMessage(lastUpdate, TramStations.of(Cornbrook), "Some long message");
        PlatformMessage infoE = createPlatformMessage(lastUpdate, TramStations.of(MediaCityUK), "Some long message");

        EasyMock.expect(platformMessageSource.messagesFor(stageE.getBoardingPlatform().getId(), lastUpdate.toLocalDate(), queryTime)).andReturn(Optional.of(infoE));
        EasyMock.expect(platformMessageSource.messagesFor(stageB.getBoardingPlatform().getId(), lastUpdate.toLocalDate(), queryTime)).andReturn(Optional.of(infoB));
        EasyMock.expect(platformMessageSource.messagesFor(stageC.getBoardingPlatform().getId(), lastUpdate.toLocalDate(), queryTime)).andReturn(Optional.of(infoC));
        EasyMock.expect(platformMessageSource.messagesFor(stageA.getBoardingPlatform().getId(), lastUpdate.toLocalDate(), queryTime)).andReturn(Optional.of(infoA));

        List<TransportStage<?,?>> stages = Arrays.asList(stageA, stageB, stageC, stageD, stageE);

        Journey journey = new Journey(queryTime.plusMinutes(5), queryTime, queryTime.plusMinutes(10), stages, Collections.emptyList(), requestedNumberChanges);

        replayAll();
        List<Note> notes = providesNotes.createNotesForJourney(journey, serviceDate);
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
        assertTrue(notes.contains(new StationNote(Live,"Some long message", of(Pomona))), notes.toString());
        assertTrue(notes.contains(new StationNote(Live,"Some long message", of(Cornbrook))), notes.toString());
        assertTrue(notes.contains(new StationNote(Live,"Some long message", of(MediaCityUK))), notes.toString());
        assertTrue(notes.contains(new StationNote(Live,"Some Location Long message", of(Altrincham))), notes.toString());
    }

    @Test
    void shouldAddNotesForStations() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        List<Station> stations = Arrays.asList(of(Pomona), of(VeloPark), of(Cornbrook));

        LocalDate localDate = LocalDate.of(2016, 10, 25);
        TramServiceDate queryDate = new TramServiceDate(localDate);

        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime());
        EasyMock.expect(platformMessageSource.messagesFor(of(Pomona), localDate, queryTime)).
                andReturn(Collections.singletonList(createPlatformMessage(lastUpdate, of(Pomona), "second message")));
        EasyMock.expect(platformMessageSource.messagesFor(of(VeloPark), localDate, queryTime)).
                andReturn(Collections.singletonList(createPlatformMessage(lastUpdate, of(VeloPark), "first message")));
        EasyMock.expect(platformMessageSource.messagesFor(of(Cornbrook), localDate, queryTime)).
                andReturn(Collections.singletonList(createPlatformMessage(lastUpdate, of(Cornbrook), "second message")));

        replayAll();

        List<Note> notes = providesNotes.createNotesForStations(stations, queryDate, queryTime);
        verifyAll();

        Assertions.assertEquals(3, notes.size());
        assertThat(notes.toString(), notes.contains(new StationNote(Live,"first message", of(VeloPark))));
        assertThat(notes.toString(), notes.contains(new StationNote(Live,"second message", of(Cornbrook))));
        assertThat(notes.toString(), notes.contains(new StationNote(Live,"second message", of(Pomona))));
    }

    private PlatformMessage createPlatformMessage(LocalDateTime lastUpdate, Station station, String message) {
        Platform platform = new Platform(station.forDTO() + "1", station.getName() + " platform 1", station.getLatLong());
        station.getBuilder().addPlatform(platform);
        return new PlatformMessage(platform.getId(), message, lastUpdate, station, "displayId");
    }

    private VehicleStage createStageWithBoardingPlatform(String platformId, LatLong latLong) {
        TramTime departTime = TramTime.of(11,22);
        Service service = new Service("serviceId");
        Trip trip = new Trip(StringIdFor.createId("tripId"), "headSign", service,
                TestEnv.getTramTestRoute());

        // TODO
        List<Integer> passedStations = new ArrayList<>();
        Platform platform = new Platform(platformId, "platformName", latLong);
        final Station firstStation = of(Ashton);
        firstStation.getBuilder().addPlatform(platform);
        VehicleStage vehicleStage = new VehicleStage(firstStation, TestEnv.getTramTestRoute(), Tram,
                trip, departTime, of(PiccadillyGardens), passedStations);
        vehicleStage.setPlatform(platform);
        return vehicleStage;
    }

}
