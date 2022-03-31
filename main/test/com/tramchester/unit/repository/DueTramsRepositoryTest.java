package com.tramchester.unit.repository;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.*;
import com.tramchester.livedata.repository.DueTramsRepository;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DueTramsRepositoryTest extends EasyMockSupport {

    private ProvidesNow providesNow;
    private DueTramsRepository repository;
    private LocalDateTime lastUpdate;
    private Station station;
    private Platform platform;

    @BeforeEach
    void beforeEachTestRuns() {
        providesNow = createMock(ProvidesNow.class);
        repository = new DueTramsRepository(providesNow, new CacheMetrics(TestEnv.NoopRegisterMetrics()));

        LocalDate today = TestEnv.LocalNow().toLocalDate();
        lastUpdate = LocalDateTime.of(today, LocalTime.of(15,42));

        platform = MutablePlatform.buildForTFGMTram("someId1", "Shudehill platform 1", Shudehill.getLatLong(), DataSourceID.unknown, IdFor.invalid());
        station = Shudehill.fakeWith(platform);
//        station.addPlatform(platform);
    }

    @Test
    void shouldCountStationsWithDueTrams() {
        List<StationDepartureInfo> infos = new ArrayList<>();

        // first station, has due tram
        DueTram dueTram = new DueTram(station, Bury.fake(), "Due", Duration.ofMinutes(42), "Single", lastUpdate.toLocalTime());
        addStationInfoWithDueTram(infos, lastUpdate, "displayId1", platform.getId(),
                "message 1", station, dueTram);

        // second station, has due tram
        Platform platfromForSecondStation = MutablePlatform.buildForTFGMTram("a1", "Altrincham platform 1", Altrincham.getLatLong(), DataSourceID.unknown, IdFor.invalid());
        Station secondStation = Altrincham.fakeWith(platfromForSecondStation);

        DueTram dueTramOther = new DueTram(secondStation, ManAirport.fake(), "Due", Duration.ofMinutes(12), "Double", lastUpdate.toLocalTime());
        addStationInfoWithDueTram(infos, lastUpdate, "displayId2", platfromForSecondStation.getId(),
                "message 2", secondStation, dueTramOther);

        // third, no due trams
        Platform platfromForThirdStation = MutablePlatform.buildForTFGMTram("b2", "Intu platform 2", TraffordCentre.getLatLong(), DataSourceID.unknown, IdFor.invalid());
        Station thirdStation = TraffordCentre.fakeWith(platfromForThirdStation);

        StationDepartureInfo thirdStationInfo = new StationDepartureInfo("displayId3", Lines.Airport,
                LineDirection.Incoming, platfromForThirdStation.getId(), thirdStation, "message 3", lastUpdate);
        infos.add(thirdStationInfo);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(3, repository.upToDateEntries());
        assertEquals(3, repository.getNumStationsWithData(lastUpdate));
        assertEquals(2, repository.getNumStationsWithTrams(lastUpdate));
    }

    @Test
    void shouldGetDepartureInformationForSingleStation() {
        List<StationDepartureInfo> infos = new ArrayList<>();

        Station destination = Bury.fake();
        DueTram dueTram = new DueTram(station, destination, "Due", Duration.ofMinutes(42), "Single", lastUpdate.toLocalTime());
        addStationInfoWithDueTram(infos, lastUpdate, "displayId", platform.getId(),
                "some message", station, dueTram);

        Platform otherPlatform = MutablePlatform.buildForTFGMTram("other1", "Altrincham platform 1", Altrincham.getLatLong(), DataSourceID.unknown, IdFor.invalid());
        Station otherStation = Altrincham.fakeWith(otherPlatform);

        Station destinationManAirport = ManAirport.fake();
        DueTram dueTramOther = new DueTram(otherStation, destinationManAirport, "Due", Duration.ofMinutes(12), "Double", lastUpdate.toLocalTime());
        addStationInfoWithDueTram(infos, lastUpdate, "displayXXX", otherPlatform.getId(),
                "some message", otherStation, dueTramOther);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(2, repository.upToDateEntries());
        assertEquals(2, repository.getNumStationsWithData(lastUpdate));

        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime());
        List<DueTram> results = repository.dueTramsForStation(station, lastUpdate.toLocalDate(), queryTime);

        assertEquals(1, results.size());
        DueTram result = results.get(0);
        assertEquals("Due", result.getStatus());
        assertMinutesEquals(42, result.getWait());
        assertEquals("Single", result.getCarriages());
        assertEquals(destination, result.getDestination());

        List<DueTram> resultOther = repository.dueTramsForStation(otherStation, lastUpdate.toLocalDate(), queryTime);
        assertEquals(1, resultOther.size());
        assertEquals(destinationManAirport, resultOther.get(0).getDestination());
    }

    @Test
    void shouldGetDepartureInformationForSingleStationDueTramOnly() {
        List<StationDepartureInfo> infos = new ArrayList<>();

        Station destination = Bury.fake();
        DueTram dueTram = new DueTram(station, destination, "Departed", Duration.ofMinutes(42), "Single", lastUpdate.toLocalTime());
        addStationInfoWithDueTram(infos, lastUpdate, "displayId", platform.getId(),
                "some message", station, dueTram);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(1, repository.upToDateEntries());
        assertEquals(1, repository.getNumStationsWithData(lastUpdate));
        assertEquals(1, repository.getNumStationsWithTrams(lastUpdate));

        TramTime queryTime = TramTime.of(lastUpdate.toLocalTime());

        Optional<PlatformDueTrams> allTramsForPlatform = repository.dueTramsForPlatform(platform.getId(), lastUpdate.toLocalDate(), queryTime);
        assertTrue(allTramsForPlatform.isPresent());

        List<DueTram> results = repository.dueTramsForStation(station, lastUpdate.toLocalDate(), queryTime);
        assertEquals(0, results.size());
    }

    @Test
    void shouldGetDueTramsWithinTimeWindows() {
        List<StationDepartureInfo> info = new LinkedList<>();

        final LocalTime lastUpdateTime = lastUpdate.toLocalTime();
        DueTram dueTram = new DueTram(station, station, "Due", Duration.ofMinutes(42), "Single", lastUpdateTime);
        addStationInfoWithDueTram(info, lastUpdate, "displayId", platform.getId(), "some message",
                station, dueTram);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        repository.updateCache(info);
        LocalDate queryDate = lastUpdate.toLocalDate();
        List<DueTram> dueTramsNow = repository.dueTramsForStation(station, queryDate, TramTime.of(lastUpdateTime));
        List<DueTram> dueTramsEarlier = repository.dueTramsForStation(station, queryDate, TramTime.of(lastUpdateTime.minusMinutes(5)));
        List<DueTram> dueTramsLater = repository.dueTramsForStation(station, queryDate, TramTime.of(lastUpdateTime.plusMinutes(5)));
        verifyAll();

        assertEquals(1, dueTramsNow.size());
        assertEquals(1, dueTramsEarlier.size());
        assertEquals(1, dueTramsLater.size());

        assertEquals(1, repository.getNumStationsWithData(lastUpdate.minusMinutes(5)));
        assertEquals(1, repository.getNumStationsWithData(lastUpdate.plusMinutes(5)));
    }

    @Test
    void shouldIgnoreDueTramOutsideTimeLimits() {
        List<StationDepartureInfo> info = new LinkedList<>();

        final LocalTime lastUpdateTime = lastUpdate.toLocalTime();
        DueTram dueTram = new DueTram(station, station, "Due", Duration.ofMinutes(42), "Single", lastUpdateTime);
        addStationInfoWithDueTram(info, lastUpdate, "displayId", platform.getId(), "some message",
                station, dueTram);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);
        LocalDateTime earlier = lastUpdate.minusMinutes(21);
        LocalDateTime later = lastUpdate.plusMinutes(21);

        replayAll();
        repository.updateCache(info);
        LocalDate queryDate = lastUpdate.toLocalDate();
        List<DueTram> dueTramsNow = repository.dueTramsForStation(station, queryDate, TramTime.of(lastUpdateTime));
        List<DueTram> dueTramsEarlier = repository.dueTramsForStation(station, queryDate, TramTime.of(earlier.toLocalTime()));
        List<DueTram> dueTramsLater = repository.dueTramsForStation(station, queryDate, TramTime.of(later.toLocalTime()));

        verifyAll();

        assertEquals(1, dueTramsNow.size());
        assertEquals(1, repository.getNumStationsWithTrams(lastUpdate));
        assertEquals(0, dueTramsEarlier.size());
        assertEquals(0, dueTramsLater.size());

        assertEquals(0, repository.getNumStationsWithData(earlier));
        assertEquals(0, repository.getNumStationsWithData(later));
        assertEquals(0, repository.getNumStationsWithTrams(earlier));
        assertEquals(0, repository.getNumStationsWithTrams(later));

    }


    static void addStationInfoWithDueTram(List<StationDepartureInfo> info, LocalDateTime lastUpdate,
                                          String displayId, IdFor<Platform> platformId, String message,
                                          Station location, DueTram dueTram) {
        StationDepartureInfo departureInfo = new StationDepartureInfo(displayId, Lines.Eccles,
                LineDirection.Incoming, platformId, location, message, lastUpdate);
        info.add(departureInfo);
        departureInfo.addDueTram(dueTram);
    }
}
