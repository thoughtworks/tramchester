package com.tramchester.unit.mappers;

import com.tramchester.TestConfig;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.livedata.LiveDataFileFetcher;
import com.tramchester.mappers.LiveDataParser;
import com.tramchester.mappers.LiveDataRouteCodeMapper;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Before;

import java.util.List;

import static org.junit.Assert.fail;

public class LiveDataRouteCodeMapperTest {

    private LiveDataRouteCodeMapper mapper;
    private LiveDataFileFetcher fetcher;

    @Before
    public void onceBeforeEachTestRuns() {
        fetcher = new LiveDataFileFetcher(TestConfig.LiveDataExampleFile);
        mapper = new LiveDataRouteCodeMapper();
    }

    @After
    public void shouldMapLiveDataStationAndLineToRouteCode() throws ParseException {
        fail("todo");
//        LiveDataParser parser = new LiveDataParser(stationRepository);
//        List<StationDepartureInfo> data = parser.parse(fetcher.fetch());




    }
}
