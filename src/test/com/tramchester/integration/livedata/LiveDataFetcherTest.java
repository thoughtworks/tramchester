package com.tramchester.integration.livedata;

import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import com.tramchester.livedata.LiveDataFetcher;
import com.tramchester.mappers.LiveDataParser;
import org.joda.time.DateTime;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

public class LiveDataFetcherTest {

    @Test
    public void shouldFetchDataFromTFGMAPI() throws ParseException {
        LiveDataFetcher liveDataFetcher = new LiveDataFetcher(new IntegrationTramTestConfig());

        String payload = liveDataFetcher.fetch();

        LiveDataParser mapper = new LiveDataParser();
        List<StationDepartureInfo> result = mapper.parse(payload);

        assertTrue(result.size()>0);

        String target = Stations.PiccadillyGardens.getId() + "1";

        Stream<StationDepartureInfo> filtered = result.stream().filter(item -> item.getStationPlatform().equals(target));

        List<StationDepartureInfo> displayInfo = filtered.collect(Collectors.toList());

        assertTrue(displayInfo.size()>0);

        StationDepartureInfo aDisplay = displayInfo.get(0);

        assertTrue(aDisplay.getMessage().length()>0);
        // this assert will fail if run at certain times of day....
        // assertTrue(aDisplay.getDueTrams().size()>0);
        assertTrue(aDisplay.getLineName().length()>0);
        DateTime when = aDisplay.getLastUpdate();
        assertEquals(DateTime.now().getDayOfMonth(),when.getDayOfMonth());
    }
}
