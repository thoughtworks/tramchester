package com.tramchester.integration.livedata;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import com.tramchester.livedata.LiveDataFetcher;
import com.tramchester.mappers.LiveDataMapper;
import org.joda.time.DateTime;
import org.json.simple.parser.ParseException;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LiveDataFetcherTest {

    @Test
    public void shouldFetchDataFromTFGMAPI() throws URISyntaxException, IOException, TramchesterException, ParseException {
        LiveDataFetcher liveDataFetcher = new LiveDataFetcher(new IntegrationTramTestConfig());

        String payload = liveDataFetcher.fetch();

        LiveDataMapper mapper = new LiveDataMapper();
        List<StationDepartureInfo> result = mapper.map(payload);

        assertTrue(result.size()>0);

        String target = Stations.PiccadillyGardens.getId() + "1";

        Stream<StationDepartureInfo> filtered = result.stream().filter(item -> item.getStationPlatform().equals(target));

        List<StationDepartureInfo> displayInfo = filtered.collect(Collectors.toList());

        assertTrue(displayInfo.size()>0);

        StationDepartureInfo aDisplay = displayInfo.get(0);

        assertTrue(aDisplay.getMessage().length()>0);
        assertTrue(aDisplay.getDueTrams().size()>0);
        assertTrue(aDisplay.getLineName().length()>0);
        DateTime when = aDisplay.getLastUpdate();
        assertEquals(DateTime.now().getDayOfMonth(),when.getDayOfMonth());

    }
}
