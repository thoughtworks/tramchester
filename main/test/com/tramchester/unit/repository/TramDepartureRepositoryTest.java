package com.tramchester.unit.repository;

import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.*;
import com.tramchester.livedata.tfgm.StationDepartureInfo;
import com.tramchester.livedata.tfgm.TramDepartureRepository;
import com.tramchester.livedata.tfgm.Lines;
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

class TramDepartureRepositoryTest extends EasyMockSupport {

    private ProvidesNow providesNow;
    private TramDepartureRepository repository;
    private LocalDateTime lastUpdate;
    private Station station;
    private Platform platform;
    private final Agency agency = TestEnv.MetAgency();
    private final TransportMode mode = TransportMode.Tram;

    @BeforeEach
    void beforeEachTestRuns() {
        providesNow = createMock(ProvidesNow.class);
        repository = new TramDepartureRepository(providesNow, new CacheMetrics(TestEnv.NoopRegisterMetrics()));

        LocalDate today = TestEnv.LocalNow().toLocalDate();
        lastUpdate = LocalDateTime.of(today, LocalTime.of(15,42));

        platform = MutablePlatform.buildForTFGMTram("someId1", "Shudehill platform 1",
                Shudehill.getLatLong(), DataSourceID.unknown, IdFor.invalid());
        station = Shudehill.fakeWith(platform);
    }

    @Test
    void shouldCountStationsWithDueTrams() {
        List<StationDepartureInfo> infos = new ArrayList<>();

        // first station, has due tram
        UpcomingDeparture dueTram = new UpcomingDeparture(station, Bury.fake(), "Due", Duration.ofMinutes(42),
                "Single", lastUpdate.toLocalTime(), agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayId1", platform.getId(),
                "message 1", station, dueTram);

        // second station, has due tram
        Platform platfromForSecondStation = MutablePlatform.buildForTFGMTram("a1", "Altrincham platform 1",
                Altrincham.getLatLong(), DataSourceID.unknown, IdFor.invalid());
        Station secondStation = Altrincham.fakeWith(platfromForSecondStation);

        UpcomingDeparture dueTramOther = new UpcomingDeparture(secondStation, ManAirport.fake(), "Due",
                Duration.ofMinutes(12), "Double", lastUpdate.toLocalTime(), agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayId2", platfromForSecondStation.getId(),
                "message 2", secondStation, dueTramOther);

        // third, no due trams
        Platform platfromForThirdStation = MutablePlatform.buildForTFGMTram("b2", "Intu platform 2",
                TraffordCentre.getLatLong(), DataSourceID.unknown, IdFor.invalid());
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
        UpcomingDeparture dueTram = new UpcomingDeparture(station, destination, "Due", Duration.ofMinutes(42),
                "Single", lastUpdate.toLocalTime(), agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayId", platform.getId(),
                "some message", station, dueTram);

        Platform otherPlatform = MutablePlatform.buildForTFGMTram("other1", "Altrincham platform 1",
                Altrincham.getLatLong(), DataSourceID.unknown, IdFor.invalid());
        Station otherStation = Altrincham.fakeWith(otherPlatform);

        Station destinationManAirport = ManAirport.fake();
        UpcomingDeparture dueTramOther = new UpcomingDeparture(otherStation, destinationManAirport, "Due",
                Duration.ofMinutes(12), "Double", lastUpdate.toLocalTime(), agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayXXX", otherPlatform.getId(),
                "some message", otherStation, dueTramOther);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(2, repository.upToDateEntries());
        assertEquals(2, repository.getNumStationsWithData(lastUpdate));

        TramTime queryTime = TramTime.ofHourMins(lastUpdate.toLocalTime());
        List<UpcomingDeparture> results = repository.dueTramsForStation(station, lastUpdate.toLocalDate(), queryTime);

        assertEquals(1, results.size());
        UpcomingDeparture result = results.get(0);
        assertEquals("Due", result.getStatus());
        assertMinutesEquals(42, result.getWait());
        assertEquals("Single", result.getCarriages());
        assertEquals(destination, result.getDestination());

        List<UpcomingDeparture> resultOther = repository.dueTramsForStation(otherStation, lastUpdate.toLocalDate(), queryTime);
        assertEquals(1, resultOther.size());
        assertEquals(destinationManAirport, resultOther.get(0).getDestination());
    }

    @Test
    void shouldGetDepartureInformationForSingleStationDueTramOnly() {
        List<StationDepartureInfo> infos = new ArrayList<>();

        Station destination = Bury.fake();
        UpcomingDeparture dueTram = new UpcomingDeparture(station, destination, "Departed", Duration.ofMinutes(42),
                "Single", lastUpdate.toLocalTime(), agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayId", platform.getId(),
                "some message", station, dueTram);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(1, repository.upToDateEntries());
        assertEquals(1, repository.getNumStationsWithData(lastUpdate));
        assertEquals(1, repository.getNumStationsWithTrams(lastUpdate));

        TramTime queryTime = TramTime.ofHourMins(lastUpdate.toLocalTime());

        Optional<PlatformDueTrams> allTramsForPlatform = repository.dueTramsForPlatform(platform.getId(),
                lastUpdate.toLocalDate(), queryTime);
        assertTrue(allTramsForPlatform.isPresent());

        List<UpcomingDeparture> results = repository.dueTramsForStation(station, lastUpdate.toLocalDate(), queryTime);
        assertEquals(0, results.size());
    }

    @Test
    void shouldGetDueTramsWithinTimeWindows() {
        List<StationDepartureInfo> info = new LinkedList<>();

        final LocalTime lastUpdateTime = lastUpdate.toLocalTime();
        UpcomingDeparture dueTram = new UpcomingDeparture(station, station, "Due", Duration.ofMinutes(42), "Single", lastUpdateTime, agency, mode);
        addStationInfoWithDueTram(info, lastUpdate, "displayId", platform.getId(), "some message",
                station, dueTram);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        repository.updateCache(info);
        LocalDate queryDate = lastUpdate.toLocalDate();
        List<UpcomingDeparture> dueTramsNow = repository.dueTramsForStation(station, queryDate,
                TramTime.ofHourMins(lastUpdateTime));
        List<UpcomingDeparture> dueTramsEarlier = repository.dueTramsForStation(station, queryDate,
                TramTime.ofHourMins(lastUpdateTime.minusMinutes(5)));
        List<UpcomingDeparture> dueTramsLater = repository.dueTramsForStation(station, queryDate,
                TramTime.ofHourMins(lastUpdateTime.plusMinutes(5)));
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
        UpcomingDeparture dueTram = new UpcomingDeparture(station, station, "Due", Duration.ofMinutes(42), "Single", lastUpdateTime, agency, mode);
        addStationInfoWithDueTram(info, lastUpdate, "displayId", platform.getId(), "some message",
                station, dueTram);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);
        LocalDateTime earlier = lastUpdate.minusMinutes(21);
        LocalDateTime later = lastUpdate.plusMinutes(21);

        replayAll();
        repository.updateCache(info);
        LocalDate queryDate = lastUpdate.toLocalDate();
        List<UpcomingDeparture> dueTramsNow = repository.dueTramsForStation(station, queryDate, TramTime.ofHourMins(lastUpdateTime));
        List<UpcomingDeparture> dueTramsEarlier = repository.dueTramsForStation(station, queryDate, TramTime.ofHourMins(earlier.toLocalTime()));
        List<UpcomingDeparture> dueTramsLater = repository.dueTramsForStation(station, queryDate, TramTime.ofHourMins(later.toLocalTime()));

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
                                          Station location, UpcomingDeparture upcomingDeparture) {
        StationDepartureInfo departureInfo = new StationDepartureInfo(displayId, Lines.Eccles,
                LineDirection.Incoming, platformId, location, message, lastUpdate);
        info.add(departureInfo);
        departureInfo.addDueTram(upcomingDeparture);
    }
}
