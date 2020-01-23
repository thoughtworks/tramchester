package com.tramchester.unit.repository;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Station;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.Stations;
import com.tramchester.livedata.LiveDataFetcher;
import com.tramchester.livedata.LiveDataHTTPFetcher;
import com.tramchester.mappers.LiveDataParser;
import com.tramchester.repository.LiveDataObserver;
import com.tramchester.repository.LiveDataRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.TestCase.*;

public class LiveDataRepositoryTest extends EasyMockSupport {

    private LiveDataFetcher fetcher;
    private LiveDataParser mapper;
    private LiveDataRepository repository;

    @Before
    public void beforeEachTestRuns() {
        fetcher = createMock(LiveDataHTTPFetcher.class);
        mapper = createMock(LiveDataParser.class);
        repository = new LiveDataRepository(fetcher, mapper);
    }

    @Test
    public void shouldGetDepartureInformationForSingleStation() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        LocalDateTime lastUpdate = LocalDateTime.now();
        StationDepartureInfo departureInfo = addStationInfo(info, lastUpdate, "displayId", "platformId",
                "some message", Stations.Altrincham);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        Station station = new Station("stationId", "area", "stopName", new LatLong(1,1), true);
        Platform platform = new Platform("platformId", "platformName");
        station.addPlatform(platform);

        replayAll();
        repository.refreshRespository();
        List<StationDepartureInfo> departures = repository.departuresFor(station);
        verifyAll();

        assertEquals(1, departures.size());
        assertEquals(departureInfo, departures.get(0));
    }

    @Test
    public void shouldUpdateStatusWhenRefreshingDataOK() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        LocalDateTime lastUpdate = LocalDateTime.now();

        addStationInfo(info, lastUpdate, "yyy", "platformIdA", "some message", Stations.Altrincham);
        addStationInfo(info, lastUpdate, "303", "platformIdB", "some message", Stations.Altrincham);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        verifyAll();

        assertEquals(2,repository.countEntries());
        assertEquals(2,repository.countMessages());

        assertEquals(0, repository.staleDataCount());
        assertEquals(2, repository.upToDateEntries(TramTime.of(lastUpdate.toLocalTime())));
        assertEquals(2, repository.upToDateEntries(TramTime.of(lastUpdate.toLocalTime().plusMinutes(14))));
        assertEquals(0, repository.upToDateEntries(TramTime.of(lastUpdate.toLocalTime().plusMinutes(16))));
    }

    @Test
    public void shouldUpdateMessageCountWhenRefreshingDataOK() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        LocalDateTime lastUpdate = LocalDateTime.now(); // up to date
        addStationInfo(info, lastUpdate, "yyy", "platformIdA", "some message", Stations.Altrincham);
        addStationInfo(info, lastUpdate, "303", "platformIdB", "<no message>", Stations.Altrincham);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        verifyAll();

        assertEquals(2,repository.countEntries());
        assertEquals(1,repository.countMessages());

    }

    @Test
    public void shouldUpdateStatusWhenRefreshingStaleData() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        LocalDateTime current = LocalDateTime.now();
        LocalDateTime staleDate = current.minusDays(5).minusMinutes(60); // stale
        addStationInfo(info, staleDate, "yyy", "platformIdC", "some message", Stations.Altrincham);
        addStationInfo(info, staleDate, "303", "platformIdD", "some message", Stations.Altrincham);
        addStationInfo(info, current, "303", "platformIdF", "some message", Stations.Altrincham);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        verifyAll();

        assertEquals(3, repository.countEntries());
        assertEquals(3, repository.countMessages());
        assertEquals(2, repository.staleDataCount());
        assertEquals(1, repository.upToDateEntries(TramTime.of(current.toLocalTime())));
    }

    public static StationDepartureInfo addStationInfo(List<StationDepartureInfo> info, LocalDateTime lastUpdate,
                                                String displayId, String platformId, String message, Station location) {
        StationDepartureInfo departureInfo = new StationDepartureInfo(displayId, "lineName", StationDepartureInfo.Direction.Incoming, platformId,
                location, message, lastUpdate);
        info.add(departureInfo);
        departureInfo.addDueTram(new DueTram(Stations.Bury, "Due", 42, "Single", lastUpdate.toLocalTime()));
        return departureInfo;
    }
}
