package com.tramchester.unit.repository;

import com.tramchester.domain.Platform;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.testSupport.Stations;
import com.tramchester.livedata.LiveDataFetcher;
import com.tramchester.livedata.LiveDataHTTPFetcher;
import com.tramchester.mappers.LiveDataParser;
import com.tramchester.repository.LiveDataRepository;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LiveDataRepositoryTest extends EasyMockSupport {

    private LiveDataFetcher fetcher;
    private LiveDataParser mapper;
    private LiveDataRepository repository;
    private ProvidesNow providesNow;
    private LocalDateTime lastUpdate;

    @BeforeEach
    void beforeEachTestRuns() {
        fetcher = createMock(LiveDataHTTPFetcher.class);
        mapper = createMock(LiveDataParser.class);
        providesNow = createMock(ProvidesNow.class);
        repository = new LiveDataRepository(fetcher, mapper, providesNow);

        lastUpdate = TestEnv.LocalNow();
    }

    @Test
    void shouldGetDepartureInformationForSingleStation() throws TransformException {
        List<StationDepartureInfo> info = new LinkedList<>();

        StationDepartureInfo departureInfo = addStationInfoWithDueTram(info, lastUpdate, "displayId", "platformId",
                "some message", Stations.Altrincham);

        EasyMock.expect(providesNow.getNow()).andStubReturn(TramTime.of(lastUpdate));
        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());
        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        Station station = Station.forTest("stationId", "area", "stopName",
                new LatLong(1,1), TransportMode.Tram);
        Platform platform = new Platform("platformId", "platformName");
        station.addPlatform(platform);

        replayAll();
        repository.refreshRespository();
        TramTime queryTime = TramTime.of(lastUpdate);
        List<StationDepartureInfo> departures = repository.departuresFor(station, TramServiceDate.of(lastUpdate), queryTime);
        verifyAll();

        assertEquals(1, departures.size());
        assertEquals(departureInfo, departures.get(0));
    }

    @Test
    void shouldGetDueTramsWithinTimeWindows() throws TransformException {
        List<StationDepartureInfo> info = new LinkedList<>();

        addStationInfoWithDueTram(info, lastUpdate, "displayId", "platformId", "some message", Stations.Altrincham);

        EasyMock.expect(providesNow.getNow()).andStubReturn(TramTime.of(lastUpdate));
        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());
        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        Station station = Station.forTest("stationId", "area", "stopName", new LatLong(1,1), TransportMode.Tram);
        Platform platform = new Platform("platformId", "platformName");
        station.addPlatform(platform);

        replayAll();
        repository.refreshRespository();
        List<DueTram> dueTramsNow = repository.dueTramsFor(station, new TramServiceDate(lastUpdate.toLocalDate()), TramTime.of(lastUpdate));
        List<DueTram> dueTramsEarlier = repository.dueTramsFor(station, new TramServiceDate(lastUpdate.toLocalDate()), TramTime.of(lastUpdate.minusMinutes(5)));
        List<DueTram> dueTramsLater = repository.dueTramsFor(station, new TramServiceDate(lastUpdate.toLocalDate()), TramTime.of(lastUpdate.plusMinutes(5)));
        verifyAll();

        assertEquals(1, dueTramsNow.size());
        assertEquals(1, dueTramsEarlier.size());
        assertEquals(1, dueTramsLater.size());
    }

    @Test
    void shouldUpdateStatusWhenRefreshingDataOK() {
        List<StationDepartureInfo> info = new LinkedList<>();

        addStationInfoWithDueTram(info, lastUpdate.plusMinutes(14), "yyy", "platformIdA",
                "some message", Stations.Altrincham);
        addStationInfoWithDueTram(info, lastUpdate.plusMinutes(21), "303", "platformIdB",
                "some message", Stations.Altrincham);

        EasyMock.expect(providesNow.getNow()).andStubReturn(TramTime.of(lastUpdate));
        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());
        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        verifyAll();

        assertEquals(1,repository.upToDateEntries());
        assertEquals(1,repository.entriesWithMessages());
        assertEquals(1, repository.missingDataCount());
    }

    @Test
    void shouldUpdateMessageCountWhenRefreshingDataOK() {
        List<StationDepartureInfo> info = new LinkedList<>();

        addStationInfoWithDueTram(info, lastUpdate, "yyy", "platformIdA", "some message", Stations.Altrincham);
        addStationInfoWithDueTram(info, lastUpdate, "303", "platformIdB", "<no message>", Stations.Altrincham);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);
        EasyMock.expect(providesNow.getNow()).andStubReturn(TramTime.of(lastUpdate));
        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());
        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        repository.refreshRespository();
        verifyAll();

        assertEquals(2,repository.upToDateEntries());
        assertEquals(1,repository.entriesWithMessages());

    }

    @Test
    void shouldUpdateStatusWhenRefreshingStaleData() {
        List<StationDepartureInfo> info = new LinkedList<>();
        Station noPlatforms = Stations.Altrincham;
        Station altrincham = new Station(noPlatforms.getId(), noPlatforms.getArea(), noPlatforms.getName(),
                noPlatforms.getLatLong(), noPlatforms.getGridPosition());

        String platformId1 = Stations.Altrincham.getId() + "1";
        String platformId2 = Stations.Altrincham.getId() + "2";

        altrincham.addPlatform(new Platform(platformId1, "Altrincham Platform 1"));
        altrincham.addPlatform(new Platform(platformId2, "Altrincham Platform 2"));

        LocalDateTime current = TestEnv.LocalNow();
        LocalDateTime staleDataAndTime = current.minusDays(5).minusMinutes(60); // stale

        addStationInfoWithDueTram(info, staleDataAndTime, "yyy", platformId1, "some message", altrincham);
        addStationInfoWithDueTram(info, staleDataAndTime, "303", platformId2, "some message", altrincham);
        addStationInfoWithDueTram(info, current, "304", "platformIdF", "some message", altrincham);

        EasyMock.expect(providesNow.getNow()).andStubReturn(TramTime.of(current));
        EasyMock.expect(providesNow.getDate()).andStubReturn(current.toLocalDate());
        EasyMock.expect(providesNow.getDateTime()).andStubReturn(current);
        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        verifyAll();

        assertEquals(1, repository.upToDateEntries());
        assertEquals(1, repository.entriesWithMessages());
        assertEquals(2, repository.missingDataCount());
        //assertEquals(1, repository.upToDateEntries()); // TramTime.of(current)

        List<DueTram> dueTrams = repository.dueTramsFor(altrincham, TramServiceDate.of(current), TramTime.of(current));
        assertEquals(0, dueTrams.size());
    }

    public static StationDepartureInfo addStationInfoWithDueTram(List<StationDepartureInfo> info, LocalDateTime lastUpdate,
                                                                 String displayId, String platformId, String message, Station location) {
        StationDepartureInfo departureInfo = new StationDepartureInfo(displayId, "lineName", StationDepartureInfo.Direction.Incoming,
                platformId, location, message, lastUpdate);
        info.add(departureInfo);
        departureInfo.addDueTram(new DueTram(Stations.Bury, "Due", 42, "Single", lastUpdate.toLocalTime()));
        return departureInfo;
    }
}
