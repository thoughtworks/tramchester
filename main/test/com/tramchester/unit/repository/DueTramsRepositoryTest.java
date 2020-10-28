package com.tramchester.unit.repository;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.PlatformDueTrams;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.DueTramsRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static com.tramchester.testSupport.TramStations.*;
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
        repository = new DueTramsRepository(providesNow);

        lastUpdate = TestEnv.LocalNow();

        station = TramStations.of(Shudehill);
        platform = new Platform("someId1", "Shudehill platform 1");
        station.addPlatform(platform);

    }

    @Test
    void shouldGetDepartureInformationForSingleStation() {
        List<StationDepartureInfo> infos = new ArrayList<>();

        Station destination = of(Bury);
        DueTram dueTram = new DueTram(destination, "Due", 42, "Single", lastUpdate.toLocalTime());
        addStationInfoWithDueTram(infos, lastUpdate, "displayId", platform.getId(),
                "some message", station, dueTram);

        Station destinationManAirport = of(ManAirport);
        DueTram dueTramOther = new DueTram(destinationManAirport, "Due", 12, "Double", lastUpdate.toLocalTime());
        Station otherStation = of(Altrincham);
        Platform otherPlatform = new Platform("other1", "Altrincham platform 1");
        otherStation.addPlatform(otherPlatform);
        addStationInfoWithDueTram(infos, lastUpdate, "displayXXX", otherPlatform.getId(),
                "some message", otherStation, dueTramOther);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(2, repository.upToDateEntries());
        assertEquals(2, repository.getNumStationsWithData(lastUpdate));

        TramTime queryTime = TramTime.of(lastUpdate);
        List<DueTram> results = repository.dueTramsFor(station, lastUpdate.toLocalDate(), queryTime);

        assertEquals(1, results.size());
        DueTram result = results.get(0);
        assertEquals("Due", result.getStatus());
        assertEquals(42, result.getWait());
        assertEquals("Single", result.getCarriages());
        assertEquals(destination, result.getDestination());

        List<DueTram> resultOther = repository.dueTramsFor(otherStation, lastUpdate.toLocalDate(), queryTime);
        assertEquals(1, resultOther.size());
        assertEquals(destinationManAirport, resultOther.get(0).getDestination());
    }

    @Test
    void shouldGetDepartureInformationForSingleStationDueTramOnly() {
        List<StationDepartureInfo> infos = new ArrayList<>();

        Station destination = of(Bury);
        DueTram dueTram = new DueTram(destination, "Departed", 42, "Single", lastUpdate.toLocalTime());
        addStationInfoWithDueTram(infos, lastUpdate, "displayId", platform.getId(),
                "some message", station, dueTram);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(1, repository.upToDateEntries());
        assertEquals(1, repository.getNumStationsWithData(lastUpdate));

        TramTime queryTime = TramTime.of(lastUpdate);

        Optional<PlatformDueTrams> allTramsForPlatform = repository.allTrams(platform.getId(), lastUpdate.toLocalDate(), queryTime);
        assertTrue(allTramsForPlatform.isPresent());

        List<DueTram> results = repository.dueTramsFor(station, lastUpdate.toLocalDate(), queryTime);
        assertEquals(0, results.size());
    }

    @Test
    void shouldGetDueTramsWithinTimeWindows() {
        List<StationDepartureInfo> info = new LinkedList<>();

        DueTram dueTram = new DueTram(station, "Due", 42, "Single", lastUpdate.toLocalTime());
        addStationInfoWithDueTram(info, lastUpdate, "displayId", platform.getId(), "some message",
                station, dueTram);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        repository.updateCache(info);
        LocalDate queryDate = lastUpdate.toLocalDate();
        List<DueTram> dueTramsNow = repository.dueTramsFor(station, queryDate, TramTime.of(lastUpdate));
        List<DueTram> dueTramsEarlier = repository.dueTramsFor(station, queryDate, TramTime.of(lastUpdate.minusMinutes(5)));
        List<DueTram> dueTramsLater = repository.dueTramsFor(station, queryDate, TramTime.of(lastUpdate.plusMinutes(5)));
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

        DueTram dueTram = new DueTram(station, "Due", 42, "Single", lastUpdate.toLocalTime());
        addStationInfoWithDueTram(info, lastUpdate, "displayId", platform.getId(), "some message",
                station, dueTram);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        repository.updateCache(info);
        LocalDate queryDate = lastUpdate.toLocalDate();
        List<DueTram> dueTramsNow = repository.dueTramsFor(station, queryDate, TramTime.of(lastUpdate));
        List<DueTram> dueTramsEarlier = repository.dueTramsFor(station, queryDate, TramTime.of(lastUpdate.minusMinutes(21)));
        List<DueTram> dueTramsLater = repository.dueTramsFor(station, queryDate, TramTime.of(lastUpdate.plusMinutes(21)));

        verifyAll();

        assertEquals(1, dueTramsNow.size());
        assertEquals(0, dueTramsEarlier.size());
        assertEquals(0, dueTramsLater.size());

        assertEquals(0, repository.getNumStationsWithData(lastUpdate.minusMinutes(21)));
        assertEquals(0, repository.getNumStationsWithData(lastUpdate.plusMinutes(21)));
    }


    public static StationDepartureInfo addStationInfoWithDueTram(List<StationDepartureInfo> info, LocalDateTime lastUpdate,
                                                                 String displayId, IdFor<Platform> platformId, String message,
                                                                 Station location, DueTram dueTram) {
        StationDepartureInfo departureInfo = new StationDepartureInfo(displayId, "lineName",
                StationDepartureInfo.Direction.Incoming, platformId, location, message, lastUpdate);
        info.add(departureInfo);
        departureInfo.addDueTram(dueTram);
        return departureInfo;
    }
}
