package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.*;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.VehicleStage;
import com.tramchester.domain.transportStages.WalkingToStationStage;
import com.tramchester.livedata.domain.liveUpdates.PlatformMessage;
import com.tramchester.livedata.repository.PlatformMessageSource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.StationHelper;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.tramchester.domain.id.StringIdFor.createId;
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

    private Station createStationFor(TramStations tramStation) {
        return StationHelper.forTest(tramStation);
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

        int year = 2021;
        LocalDate date = LocalDate.of(year, 12, 23);
        Note christmasNote = new Note(ProvidesNotes.christmas2021, Note.NoteType.Christmas);

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

        PlatformMessage info = createPlatformMessage(lastUpdate, Pomona, "<no message>");
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

        PlatformMessage info = createPlatformMessage(lastUpdate, Pomona, "a message");
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

        PlatformMessage info = createPlatformMessage(lastUpdate, Pomona, "a message");

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
        WalkingToStationStage stageD = new WalkingToStationStage(new MyLocation(TestEnv.nearAltrincham),
                TramStations.of(Ashton), 7, TramTime.of(8,11));
        VehicleStage stageE = createStageWithBoardingPlatform("platformId5", Altrincham.getLatLong());

        TramServiceDate serviceDate = new TramServiceDate(lastUpdate.toLocalDate());
        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime());

        PlatformMessage infoA = createPlatformMessage(lastUpdate, Pomona, "Some long message");
        PlatformMessage infoB = createPlatformMessage(lastUpdate, Altrincham, "Some Location Long message");
        PlatformMessage infoC = createPlatformMessage(lastUpdate, Cornbrook, "Some long message");
        PlatformMessage infoE = createPlatformMessage(lastUpdate, MediaCityUK, "Some long message");

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
        assertTrue(notes.contains(new StationNote(Live,"Some long message", createStationFor(Pomona))), notes.toString());
        assertTrue(notes.contains(new StationNote(Live,"Some long message", createStationFor(Cornbrook))), notes.toString());
        assertTrue(notes.contains(new StationNote(Live,"Some long message", createStationFor(MediaCityUK))), notes.toString());
        assertTrue(notes.contains(new StationNote(Live,"Some Location Long message", createStationFor(Altrincham))), notes.toString());
    }

    @Test
    void shouldAddNotesForStations() {
        EasyMock.expect(platformMessageSource.isEnabled()).andReturn(true);

        List<Station> stations = Arrays.asList(createStationFor(Pomona), createStationFor(VeloPark), createStationFor(Cornbrook));

        LocalDate localDate = LocalDate.of(2016, 10, 25);
        TramServiceDate queryDate = new TramServiceDate(localDate);

        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime());
        EasyMock.expect(platformMessageSource.messagesFor(createStationFor(Pomona), localDate, queryTime)).
                andReturn(Collections.singletonList(createPlatformMessage(lastUpdate, Pomona, "second message")));
        EasyMock.expect(platformMessageSource.messagesFor(createStationFor(VeloPark), localDate, queryTime)).
                andReturn(Collections.singletonList(createPlatformMessage(lastUpdate, VeloPark, "first message")));
        EasyMock.expect(platformMessageSource.messagesFor(createStationFor(Cornbrook), localDate, queryTime)).
                andReturn(Collections.singletonList(createPlatformMessage(lastUpdate, Cornbrook, "second message")));

        replayAll();

        List<Note> notes = providesNotes.createNotesForStations(stations, queryDate, queryTime);
        verifyAll();

        Assertions.assertEquals(3, notes.size());
        assertThat(notes.toString(), notes.contains(new StationNote(Live,"first message", createStationFor(VeloPark))));
        assertThat(notes.toString(), notes.contains(new StationNote(Live,"second message", createStationFor(Cornbrook))));
        assertThat(notes.toString(), notes.contains(new StationNote(Live,"second message", createStationFor(Pomona))));
    }

//    private PlatformMessage createPlatformMessage(LocalDateTime lastUpdate, MutableStation station, String message) {
//        Platform platform = MutablePlatform.buildForTFGMTram(station.forDTO() + "1", station.getName() + " platform 1", station.getLatLong());
//        station.addPlatform(platform);
//        return new PlatformMessage(platform.getId(), message, lastUpdate, station, "displayId");
//    }

    private PlatformMessage createPlatformMessage(LocalDateTime lastUpdate, TramStations tramStation, String message) {

        Platform platform = MutablePlatform.buildForTFGMTram(tramStation.getRawId() + "1", tramStation.getName() + " platform 1",
                tramStation.getLatLong());
        Station station = StationHelper.forTest(tramStation, platform);
        //station.addPlatform(platform);

        return new PlatformMessage(platform.getId(), message, lastUpdate, station, "displayId");
    }

    private VehicleStage createStageWithBoardingPlatform(String platformId, LatLong latLong) {
        TramTime departTime = TramTime.of(11,22);
        Service service = MutableService.build(createId("serviceId"));
        Trip trip = MutableTrip.build(createId("tripId"), "headSign", service,
                TestEnv.getTramTestRoute());

        // TODO
        List<Integer> passedStations = new ArrayList<>();

        Platform platform = MutablePlatform.buildForTFGMTram(platformId, "platformName", latLong);
        final Station firstStation = StationHelper.forTest(Ashton, platform);
        //firstStation.addPlatform(platform);

        VehicleStage vehicleStage = new VehicleStage(firstStation, TestEnv.getTramTestRoute(), Tram,
                trip, departTime, createStationFor(PiccadillyGardens), passedStations);

        vehicleStage.setPlatform(platform);
        return vehicleStage;
    }

}
