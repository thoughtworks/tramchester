package com.tramchester.unit.repository;

import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.tfgm.Lines;
import com.tramchester.livedata.tfgm.TramDepartureRepository;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
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

import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TramDepartureRepositoryTest extends EasyMockSupport {

    private ProvidesNow providesNow;
    private TramDepartureRepository repository;
    private LocalDateTime lastUpdate;
    private Station station;
    private Platform platform;
    private final Agency agency = TestEnv.MetAgency();
    private final TransportMode mode = TransportMode.Tram;
    private LocalDate date;

    @BeforeEach
    void beforeEachTestRuns() {
        providesNow = createMock(ProvidesNow.class);
        repository = new TramDepartureRepository(providesNow, new CacheMetrics(TestEnv.NoopRegisterMetrics()));

        LocalDate today = TestEnv.LocalNow().toLocalDate();
        date = today;
        lastUpdate = LocalDateTime.of(today, LocalTime.of(15,42));

        platform = MutablePlatform.buildForTFGMTram("someId1", "Shudehill platform 1",
                Shudehill.getLatLong(), DataSourceID.unknown, IdFor.invalid());
        station = Shudehill.fakeWith(platform);
    }

    @Test
    void shouldCountStationsWithDueTrams() {
        List<TramStationDepartureInfo> infos = new ArrayList<>();

        // first station, has due tram
        UpcomingDeparture dueTram = new UpcomingDeparture(date, station, Bury.fake(), "Due", Duration.ofMinutes(42),
                "Single", lastUpdate.toLocalTime(), agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayId1", platform.getId(),
                "message 1", station, dueTram);

        // second station, has due tram
        Platform platfromForSecondStation = MutablePlatform.buildForTFGMTram("a1", "Altrincham platform 1",
                Altrincham.getLatLong(), DataSourceID.unknown, IdFor.invalid());
        Station secondStation = Altrincham.fakeWith(platfromForSecondStation);

        UpcomingDeparture dueTramOther = new UpcomingDeparture(date, secondStation, ManAirport.fake(), "Due",
                Duration.ofMinutes(12), "Double", lastUpdate.toLocalTime(), agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayId2", platfromForSecondStation.getId(),
                "message 2", secondStation, dueTramOther);

        // third, no due trams
        Platform platfromForThirdStation = MutablePlatform.buildForTFGMTram("b2", "Intu platform 2",
                TraffordCentre.getLatLong(), DataSourceID.unknown, IdFor.invalid());
        Station thirdStation = TraffordCentre.fakeWith(platfromForThirdStation);

        TramStationDepartureInfo thirdStationInfo = new TramStationDepartureInfo("displayId3", Lines.Airport,
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
        List<TramStationDepartureInfo> infos = new ArrayList<>();

        Station destination = Bury.fake();
        UpcomingDeparture dueTram = new UpcomingDeparture(date, station, destination, "Due", Duration.ofMinutes(42),
                "Single", lastUpdate.toLocalTime(), agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayId", platform.getId(),
                "some message", station, dueTram);

        Platform otherPlatform = MutablePlatform.buildForTFGMTram("other1", "Altrincham platform 1",
                Altrincham.getLatLong(), DataSourceID.unknown, IdFor.invalid());
        Station otherStation = Altrincham.fakeWith(otherPlatform);

        Station destinationManAirport = ManAirport.fake();
        UpcomingDeparture dueTramOther = new UpcomingDeparture(date, otherStation, destinationManAirport, "Due",
                Duration.ofMinutes(12), "Double", lastUpdate.toLocalTime(), agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayXXX", otherPlatform.getId(),
                "some message", otherStation, dueTramOther);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(2, repository.upToDateEntries());
        assertEquals(2, repository.getNumStationsWithData(lastUpdate));

        List<UpcomingDeparture> results = repository.dueTramsForStation(station);

        assertEquals(1, results.size());
        UpcomingDeparture result = results.get(0);
        assertEquals("Due", result.getStatus());
        assertMinutesEquals(42, result.getWait());
        assertEquals("Single", result.getCarriages());
        assertEquals(destination, result.getDestination());

        List<UpcomingDeparture> resultOther = repository.dueTramsForStation(otherStation);
        assertEquals(1, resultOther.size());
        assertEquals(destinationManAirport, resultOther.get(0).getDestination());
    }

    @Test
    void shouldGetDepartureInformationForSingleStationDueTramOnly() {
        List<TramStationDepartureInfo> infos = new ArrayList<>();

        Station destination = Bury.fake();
        UpcomingDeparture dueTram = new UpcomingDeparture(date, station, destination, "Departed", Duration.ofMinutes(42),
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

        List<UpcomingDeparture> allTramsForPlatform = repository.dueTramsForPlatform(platform.getId());
        assertFalse(allTramsForPlatform.isEmpty());

        List<UpcomingDeparture> results = repository.dueTramsForStation(station);
        assertEquals(0, results.size());
    }

    @Test
    void shouldGetDueTramsWithinTimeWindows() {
        List<TramStationDepartureInfo> info = new LinkedList<>();

        final LocalTime lastUpdateTime = lastUpdate.toLocalTime();
        UpcomingDeparture dueTram = new UpcomingDeparture(date, station, station, "Due", Duration.ofMinutes(42), "Single", lastUpdateTime, agency, mode);
        addStationInfoWithDueTram(info, lastUpdate, "displayId", platform.getId(), "some message",
                station, dueTram);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        repository.updateCache(info);
        List<UpcomingDeparture> dueTramsNow = repository.dueTramsForStation(station);
        List<UpcomingDeparture> dueTramsEarlier = repository.dueTramsForStation(station);
        List<UpcomingDeparture> dueTramsLater = repository.dueTramsForStation(station);
        verifyAll();

        assertEquals(1, dueTramsNow.size());
        assertEquals(1, dueTramsEarlier.size());
        assertEquals(1, dueTramsLater.size());

        assertEquals(1, repository.getNumStationsWithData(lastUpdate.minusMinutes(5)));
        assertEquals(1, repository.getNumStationsWithData(lastUpdate.plusMinutes(5)));
    }

    static void addStationInfoWithDueTram(List<TramStationDepartureInfo> info, LocalDateTime lastUpdate,
                                          String displayId, IdFor<Platform> platformId, String message,
                                          Station location, UpcomingDeparture upcomingDeparture) {
        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo(displayId, Lines.Eccles,
                LineDirection.Incoming, platformId, location, message, lastUpdate);
        info.add(departureInfo);
        departureInfo.addDueTram(upcomingDeparture);
    }
}
