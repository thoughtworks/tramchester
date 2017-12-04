package com.tramchester.unit.repository;

import com.tramchester.domain.Platform;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.livedata.LiveDataFetcher;
import com.tramchester.mappers.LiveDataMapper;
import com.tramchester.repository.LiveDataRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.DateTime;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class LiveDataRepositoryTest extends EasyMockSupport {

    private LiveDataFetcher fetcher;
    private LiveDataMapper mapper;
    private LiveDataRepository repository;

    @Before
    public void beforeEachTestRuns() {
        fetcher = createMock(LiveDataFetcher.class);
        mapper = createMock(LiveDataMapper.class);
        repository = new LiveDataRepository(fetcher, mapper);

    }

    @Test
    public void shouldPopulateRepository() throws TramchesterException, IOException, URISyntaxException, ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.map("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        verifyAll();
    }

    @Test
    public void shouldEnrichAPlatform() throws TramchesterException, IOException, URISyntaxException, ParseException {
        List<StationDepartureInfo> info = new LinkedList<>();

        StationDepartureInfo departureInfo = new StationDepartureInfo("lineName", "platformId", "some message", DateTime.now());
        info.add(departureInfo);
        departureInfo.addDueTram(new DueTram("dest", "Due", 42, "Single"));

        Platform platform = new Platform("platformId", "Platform name");

        EasyMock.expect(fetcher.fetch()).andReturn("someData");
        EasyMock.expect(mapper.map("someData")).andReturn(info);

        replayAll();
        repository.refreshRespository();
        repository.enrich(platform);
        verifyAll();

        StationDepartureInfo enriched = platform.getDepartureInfo();

        assertEquals(departureInfo, enriched);
    }
}
