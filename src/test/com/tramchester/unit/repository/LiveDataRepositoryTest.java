package com.tramchester.unit.repository;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Station;
import com.tramchester.domain.TimeAsMinutes;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.Stations;
import com.tramchester.livedata.LiveDataFetcher;
import com.tramchester.mappers.LiveDataMapper;
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
    private LiveDataMapper mapper;
    private LiveDataRepository repository;
    private PlatformDTO platform;

    @Before
    public void beforeEachTestRuns() {
        fetcher = createMock(LiveDataFetcher.class);
        mapper = createMock(LiveDataMapper.class);
        repository = new LiveDataRepository(fetcher, mapper);
        platform = new PlatformDTO(new Platform("platformId", "Platform name"));
    }

    @Test
    public void shouldPopulateRepository() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.map("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        verifyAll();
    }

    @Test
    public void shouldEnrichAPlatformWhenDateAndTimeWithinTimeRange() throws ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        DateTime lastUpdate = DateTime.now();
        StationDepartureInfo departureInfo = createStationDepartureInfo(info, lastUpdate);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.map("someData")).andReturn(info);

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
        createStationDepartureInfo(info, lastUpdate);

        EasyMock.expect(fetcher.fetch()).andStubReturn("someData");
        EasyMock.expect(mapper.map("someData")).andStubReturn(info);

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
        createStationDepartureInfo(info, lastUpdate);

        EasyMock.expect(fetcher.fetch()).andStubReturn("someData");
        EasyMock.expect(mapper.map("someData")).andStubReturn(info);

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
        StationDepartureInfo departureInfo = createStationDepartureInfo(info, lastUpdate);

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.map("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        repository.enrich(locationDTO, DateTime.now() );
        verifyAll();

        StationDepartureInfo result = locationDTO.getPlatforms().get(0).getStationDepartureInfo();
        assertNotNull(result);
        assertEquals(departureInfo, result);
    }

    private StationDepartureInfo createStationDepartureInfo(List<StationDepartureInfo> info, DateTime lastUpdate) {
        StationDepartureInfo departureInfo = new StationDepartureInfo("displayId", "lineName", "platformId",
                "some message", lastUpdate);
        info.add(departureInfo);
        departureInfo.addDueTram(new DueTram("dest", "Due", 42, "Single", lastUpdate));
        return departureInfo;
    }
}
