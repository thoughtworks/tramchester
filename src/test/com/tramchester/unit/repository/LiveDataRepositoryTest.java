package com.tramchester.unit.repository;

import com.tramchester.domain.*;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.livedata.LiveDataFetcher;
import com.tramchester.livedata.LiveDataHTTPFetcher;
import com.tramchester.mappers.LiveDataParser;
import com.tramchester.repository.LiveDataRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static junit.framework.TestCase.*;

public class LiveDataRepositoryTest extends EasyMockSupport {

    private LiveDataFetcher fetcher;
    private LiveDataParser mapper;
    private LiveDataRepository repository;
    private PlatformDTO platformDTO;

    @Before
    public void beforeEachTestRuns() {
        fetcher = createMock(LiveDataHTTPFetcher.class);
        mapper = createMock(LiveDataParser.class);
        repository = new LiveDataRepository(fetcher, mapper);
        platformDTO = new PlatformDTO(new Platform("platformId", "Platform name"));
    }

    @Test
    public void shouldGetDepartureInformationForSingleStation() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        DateTime lastUpdate = DateTime.now();
        StationDepartureInfo departureInfo = addStationInfo(info, lastUpdate, "displayId", "platformId",
                "some message", "platformLocation");

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
    public void shouldExcludeMessageBoardTextForSomeDisplays() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        PlatformDTO platformA = new PlatformDTO(new Platform("platformIdA", "Platform name"));
        PlatformDTO platformB = new PlatformDTO(new Platform("platformIdB", "Platform name"));

        DateTime lastUpdate = DateTime.now();
        addStationInfo(info, lastUpdate, "yyy", "platformIdA", "some message", "platformLocation");
        addStationInfo(info, lastUpdate, "303", "platformIdB", "exclude message", "platformLocation");

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        verifyAll();

        assertEquals(2,repository.count());
        TramServiceDate queryDate = new TramServiceDate(lastUpdate.toLocalDate());
        TramTime queryTime = TramTime.create(lastUpdate.toLocalTime());
        repository.enrich(platformA, queryDate, queryTime);
        repository.enrich(platformB, queryDate, queryTime);

        assertEquals("some message", platformA.getStationDepartureInfo().getMessage());
        assertEquals("", platformB.getStationDepartureInfo().getMessage());
    }

    @Test
    public void shouldUpdateStatusWhenRefreshingDataOK() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        DateTime lastUpdate = DateTime.now(); // up to date
        addStationInfo(info, lastUpdate, "yyy", "platformIdA", "some message", "platformLocation");
        addStationInfo(info, lastUpdate, "303", "platformIdB", "exclude message", "platformLocation");

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        verifyAll();

        assertEquals(2,repository.count());
        assertEquals(0, repository.staleDataCount());
    }

    @Test
    public void shouldUpdateStatusWhenRefreshingStaleData() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        DateTime lastUpdate = DateTime.now().minusDays(5); // stale
        addStationInfo(info, lastUpdate, "yyy", "platformIdC", "some message", "platformLocation");
        addStationInfo(info, lastUpdate, "303", "platformIdD", "exclude message", "platformLocation");
        addStationInfo(info, DateTime.now(), "303", "platformIdF", "exclude message", "platformLocation");

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        verifyAll();

        assertEquals(3,repository.count());
        assertEquals(2, repository.staleDataCount());
    }

    @Test
    public void shouldEnrichAPlatformWhenDateAndTimeWithinTimeRange() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        DateTime lastUpdate = DateTime.now();
        StationDepartureInfo departureInfo = addStationInfo(info, lastUpdate, "displayId", "platformId",
                "some message", "platformLocation");

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        TramServiceDate queryDate = new TramServiceDate(lastUpdate.toLocalDate());
        repository.enrich(platformDTO, queryDate, TramTime.create(lastUpdate.toLocalTime()));
        verifyAll();

        StationDepartureInfo enriched = platformDTO.getStationDepartureInfo();

        assertEquals(departureInfo, enriched);
    }

    @Test
    public void shouldNotEnrichAPlatformWhenDateOutsideOfRange() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        DateTime lastUpdate = DateTime.now();
        addStationInfo(info, lastUpdate, "displayId", "platformId", "some message", "platformLocation");

        EasyMock.expect(fetcher.fetch()).andStubReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andStubReturn(info);

        TramServiceDate queryDateA = new TramServiceDate(LocalDate.now().minusDays(1));
        TramServiceDate queryDateB = new TramServiceDate(LocalDate.now().plusDays(1));

        replayAll();
        repository.refreshRespository();
        TramTime queryTime = TramTime.create(lastUpdate.toLocalTime());
        repository.enrich(platformDTO, queryDateA, queryTime);
        StationDepartureInfo enriched = platformDTO.getStationDepartureInfo();
        assertTrue(enriched==null);

        repository.enrich(platformDTO, queryDateB, queryTime);
        enriched = platformDTO.getStationDepartureInfo();
        assertTrue(enriched==null);
        verifyAll();
    }

    @Test
    public void shouldNotEnrichAPlatformWhenTimeOutsideOfRange() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        DateTime lastUpdate = DateTime.now();
        addStationInfo(info, lastUpdate, "displayId", "platformId", "some message", "platformLocation");

        EasyMock.expect(fetcher.fetch()).andStubReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andStubReturn(info);

        TramServiceDate queryDate = new TramServiceDate(LocalDate.now());

        replayAll();
        repository.refreshRespository();
        TramTime queryTime = TramTime.create(lastUpdate.toLocalTime());
        repository.enrich(platformDTO, queryDate, queryTime.plusMinutes(LiveDataRepository.TIME_LIMIT));
        StationDepartureInfo enriched = platformDTO.getStationDepartureInfo();
        assertTrue(enriched==null);

        repository.enrich(platformDTO, queryDate, queryTime.minusMinutes(LiveDataRepository.TIME_LIMIT));
        enriched = platformDTO.getStationDepartureInfo();
        assertTrue(enriched==null);
        verifyAll();
    }

    @Test
    public void shouldEnrichLocationDTOIfHasPlatforms() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        LatLong latLong = new LatLong(-1,2);
        Station station = new Station("id", "area", "|stopName", latLong, true);
        station.addPlatform(new Platform("platformId", "Platform name"));
        LocationDTO locationDTO = new LocationDTO(station);

        DateTime lastUpdate = DateTime.now();
        StationDepartureInfo departureInfo = addStationInfo(info, lastUpdate, "displayId",
                "platformId", "some message", "platformLocation");

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        repository.enrich(locationDTO, DateTime.now() );
        verifyAll();

        StationDepartureInfo result = locationDTO.getPlatforms().get(0).getStationDepartureInfo();
        assertNotNull(result);
        assertEquals(departureInfo, result);
    }

    private StationDepartureInfo addStationInfo(List<StationDepartureInfo> info, DateTime lastUpdate,
                                                String displayId, String platformId, String message, String location) {
        StationDepartureInfo departureInfo = new StationDepartureInfo(displayId, "lineName", platformId,
                location, message, lastUpdate);
        info.add(departureInfo);
        departureInfo.addDueTram(new DueTram("dest", "Due", 42, "Single", lastUpdate.toLocalTime()));
        return departureInfo;
    }
}
