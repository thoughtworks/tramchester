package com.tramchester.unit.repository;

import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
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
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TramDepartureRepositoryTest extends EasyMockSupport {

    private ProvidesNow providesNow;
    private TramDepartureRepository departureRepository;
    private LocalDateTime lastUpdate;
    private Station station;
    private Platform platform;
    private final Agency agency = TestEnv.MetAgency();
    private final TransportMode mode = TransportMode.Tram;
    private LocalDate date;

    @BeforeEach
    void beforeEachTestRuns() {
        providesNow = createMock(ProvidesNow.class);
        departureRepository = new TramDepartureRepository(providesNow, new CacheMetrics(TestEnv.NoopRegisterMetrics()));

        LocalDate today = TestEnv.LocalNow().toLocalDate();
        date = today;
        lastUpdate = LocalDateTime.of(today, LocalTime.of(15,42));

        station = Shudehill.fakeWithPlatform("someId1", Shudehill.getLatLong(),
                DataSourceID.unknown, IdFor.invalid());
        platform = TestEnv.onlyPlatform(station);
    }

    @Test
    void shouldCountStationsWithDueTrams() {
        List<TramStationDepartureInfo> infos = new ArrayList<>();

        // first station, has due tram
        UpcomingDeparture dueTram = new UpcomingDeparture(date, station, Bury.fake(), "Due", Duration.ofMinutes(42),
                "Single", lastUpdate.toLocalTime(), agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayId1", platform,
                "message 1", station, dueTram);

        // second station, has due tram
        Station secondStation = Altrincham.fakeWithPlatform("a1", Altrincham.getLatLong(), DataSourceID.unknown,
                IdFor.invalid());
        Platform platfromForSecondStation = TestEnv.onlyPlatform(secondStation);

        UpcomingDeparture dueTramOther = new UpcomingDeparture(date, secondStation, ManAirport.fake(), "Due",
        Duration.ofMinutes(12), "Double", lastUpdate.toLocalTime(), agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayId2", platfromForSecondStation,
                "message 2", secondStation, dueTramOther);

        // third, no due trams
        Station thirdStation = TraffordCentre.fakeWithPlatform("b2", TraffordCentre.getLatLong(),
                DataSourceID.unknown, IdFor.invalid());
        Platform platfromForThirdStation = TestEnv.onlyPlatform(thirdStation);

        TramStationDepartureInfo thirdStationInfo = new TramStationDepartureInfo("displayId3", Lines.Airport,
        LineDirection.Incoming, thirdStation, "message 3", lastUpdate);
        thirdStationInfo.setStationPlatform(platfromForThirdStation);
        infos.add(thirdStationInfo);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        departureRepository.updateCache(infos);
        verifyAll();

        assertEquals(3, departureRepository.upToDateEntries());
        assertEquals(3, departureRepository.getNumStationsWithData(lastUpdate));
        assertEquals(2, departureRepository.getNumStationsWithTrams(lastUpdate));
    }

    @Test
    void shouldGetDepartureInformationForSingleStation() {
        List<TramStationDepartureInfo> infos = new ArrayList<>();

        Station destination = Bury.fake();
        UpcomingDeparture dueTram = new UpcomingDeparture(date, station, destination, "Due", Duration.ofMinutes(42),
                "Single", lastUpdate.toLocalTime(), agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayId", platform,
                "some message", station, dueTram);

        Station otherStation = Altrincham.fakeWithPlatform("other1", Altrincham.getLatLong(),
                DataSourceID.unknown, IdFor.invalid());
        Platform otherPlatform = TestEnv.onlyPlatform(otherStation);

                Station destinationManAirport = ManAirport.fake();
        UpcomingDeparture dueTramOther = new UpcomingDeparture(date, otherStation, destinationManAirport, "Due",
                Duration.ofMinutes(12), "Double", lastUpdate.toLocalTime(), agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayXXX", otherPlatform,
                "some message", otherStation, dueTramOther);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        departureRepository.updateCache(infos);
        verifyAll();

        assertEquals(2, departureRepository.upToDateEntries());
        assertEquals(2, departureRepository.getNumStationsWithData(lastUpdate));

        List<UpcomingDeparture> results = departureRepository.forStation(station);

        assertEquals(1, results.size());
        UpcomingDeparture result = results.get(0);
        assertEquals("Due", result.getStatus());
        assertMinutesEquals(42, result.getWait());
        assertEquals("Single", result.getCarriages());
        assertEquals(destination, result.getDestination());

        List<UpcomingDeparture> resultOther = departureRepository.forStation(otherStation);
        assertEquals(1, resultOther.size());
        assertEquals(destinationManAirport, resultOther.get(0).getDestination());
    }

    @Test
    void shouldGetDepartureInformationForSingleStationDueTramOnly() {
        List<TramStationDepartureInfo> infos = new ArrayList<>();

        Station destination = Bury.fake();
        UpcomingDeparture dueTram = new UpcomingDeparture(date, station, destination, "Due", Duration.ofMinutes(42),
                "Single", lastUpdate.toLocalTime(), agency, mode);
        dueTram.setPlatform(platform);

        addStationInfoWithDueTram(infos, lastUpdate, "displayId", platform,
                "some message", station, dueTram);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        departureRepository.updateCache(infos);
        verifyAll();

        assertEquals(1, departureRepository.upToDateEntries());
        assertEquals(1, departureRepository.getNumStationsWithData(lastUpdate));
        assertEquals(1, departureRepository.getNumStationsWithTrams(lastUpdate));


        List<UpcomingDeparture> allTramsForStation = departureRepository.forStation(station);

        List<UpcomingDeparture> allTramsForPlatform = allTramsForStation.stream().
                filter(departure -> departure.getPlatform().equals(platform))
                        .collect(Collectors.toList());

        assertFalse(allTramsForPlatform.isEmpty());

    }

    @Test
    void shouldGetDueTramsWithinTimeWindows() {
        List<TramStationDepartureInfo> info = new LinkedList<>();

        final LocalTime lastUpdateTime = lastUpdate.toLocalTime();
        UpcomingDeparture dueTram = new UpcomingDeparture(date, station, station, "Due", Duration.ofMinutes(42), "Single", lastUpdateTime, agency, mode);
        addStationInfoWithDueTram(info, lastUpdate, "displayId", platform, "some message",
                station, dueTram);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        departureRepository.updateCache(info);
        List<UpcomingDeparture> dueTramsNow = departureRepository.forStation(station);
        List<UpcomingDeparture> dueTramsEarlier = departureRepository.forStation(station);
        List<UpcomingDeparture> dueTramsLater = departureRepository.forStation(station);
        verifyAll();

        assertEquals(1, dueTramsNow.size());
        assertEquals(1, dueTramsEarlier.size());
        assertEquals(1, dueTramsLater.size());

        assertEquals(1, departureRepository.getNumStationsWithData(lastUpdate.minusMinutes(5)));
        assertEquals(1, departureRepository.getNumStationsWithData(lastUpdate.plusMinutes(5)));
    }

    static void addStationInfoWithDueTram(List<TramStationDepartureInfo> info, LocalDateTime lastUpdate,
                                          String displayId, Platform platform,
                                          String message,
                                          Station location, UpcomingDeparture upcomingDeparture) {
        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo(displayId, Lines.Eccles,
                LineDirection.Incoming, location, message, lastUpdate);
        departureInfo.setStationPlatform(platform);
        departureInfo.addDueTram(upcomingDeparture);
        departureInfo.setStationPlatform(platform);

        info.add(departureInfo);

    }
}
