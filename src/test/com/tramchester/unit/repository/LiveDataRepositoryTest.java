package com.tramchester.unit.repository;

import com.tramchester.domain.*;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.livedata.LiveDataFetcher;
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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class LiveDataRepositoryTest extends EasyMockSupport {

    private LiveDataFetcher fetcher;
    private LiveDataParser mapper;
    private LiveDataRepository repository;
    private PlatformDTO platform;

    @Before
    public void beforeEachTestRuns() {
        fetcher = createMock(LiveDataFetcher.class);
        mapper = createMock(LiveDataParser.class);
        repository = new LiveDataRepository(fetcher, mapper);
        platform = new PlatformDTO(new Platform("platformId", "Platform name"));
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
        repository.enrich(platformA, queryDate, TimeAsMinutes.getMinutes(lastUpdate.toLocalTime()));
        repository.enrich(platformB, queryDate, TimeAsMinutes.getMinutes(lastUpdate.toLocalTime()));

        assertEquals("some message", platformA.getStationDepartureInfo().getMessage());
        assertEquals("", platformB.getStationDepartureInfo().getMessage());
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
        repository.enrich(platform, queryDate, TimeAsMinutes.getMinutes(lastUpdate.toLocalTime()));
        verifyAll();

        StationDepartureInfo enriched = platform.getStationDepartureInfo();

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
        int queryMins = TimeAsMinutes.getMinutes(lastUpdate.toLocalTime());
        repository.enrich(platform, queryDateA, queryMins);
        StationDepartureInfo enriched = platform.getStationDepartureInfo();
        assertTrue(enriched==null);

        repository.enrich(platform, queryDateB, queryMins);
        enriched = platform.getStationDepartureInfo();
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
        int queryMins = TimeAsMinutes.getMinutes(lastUpdate.toLocalTime());
        repository.enrich(platform, queryDate, queryMins+LiveDataRepository.TIME_LIMIT);
        StationDepartureInfo enriched = platform.getStationDepartureInfo();
        assertTrue(enriched==null);

        repository.enrich(platform, queryDate, queryMins-LiveDataRepository.TIME_LIMIT);
        enriched = platform.getStationDepartureInfo();
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

    private StationDepartureInfo addStationInfo(List<StationDepartureInfo> info, DateTime lastUpdate, String displayId,
                                                String platformId, String message, String location) {
        StationDepartureInfo departureInfo = new StationDepartureInfo(displayId, "lineName", platformId,
                location, message, lastUpdate);
        info.add(departureInfo);
        TramTime tramTime = TramTime.create(lastUpdate.getHourOfDay(), lastUpdate.getMinuteOfHour());

        departureInfo.addDueTram(new DueTram("dest", "Due", 42, "Single", tramTime));
        return departureInfo;
    }
}
