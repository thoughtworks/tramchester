package com.tramchester.integration.livedata;

import com.tramchester.Dependencies;
import com.tramchester.LiveDataTestCategory;
import com.tramchester.TestConfig;
import com.tramchester.domain.Station;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import com.tramchester.livedata.LiveDataFileFetcher;
import com.tramchester.livedata.LiveDataHTTPFetcher;
import com.tramchester.mappers.LiveDataParser;
import com.tramchester.repository.TransportDataFromFiles;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.*;
import org.junit.experimental.categories.Category;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LiveDataHTTPFetcherTest {

    private static Dependencies dependencies;
    private static LiveDataHTTPFetcher fetcher;
    private static String payload;

    private TransportDataFromFiles transportData;
    private List<StationDepartureInfo> departureInfos;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
        // don't want to fetch every time
        fetcher = dependencies.get(LiveDataHTTPFetcher.class);
        payload = fetcher.fetch();
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() throws ParseException {
        transportData = dependencies.get(TransportDataFromFiles.class);
        LiveDataParser parser = dependencies.get(LiveDataParser.class);
        departureInfos = parser.parse(payload);
    }

    @Test
    @Category(LiveDataTestCategory.class)
    public void shouldFetchDataFromTFGMAPI() {
        assertTrue(departureInfos.size()>0);

        String target = Stations.PiccadillyGardens.getId() + "1";

        Stream<StationDepartureInfo> filtered = departureInfos.stream().filter(item -> item.getStationPlatform().equals(target));

        List<StationDepartureInfo> displayInfo = filtered.collect(Collectors.toList());

        assertTrue(displayInfo.size()>0);

        StationDepartureInfo aDisplay = displayInfo.get(0);

        assertTrue(aDisplay.getMessage().length()>0);
        // this assert will fail if run at certain times of day....
        // assertTrue(aDisplay.getDueTrams().size()>0);
        assertTrue(aDisplay.getLineName().length()>0);
        LocalDateTime when = aDisplay.getLastUpdate();
        assertEquals(LocalDateTime.now().getDayOfMonth(),when.getDayOfMonth());
    }

    @Test
    @Category(LiveDataTestCategory.class)
    public void shouldHaveCrosscheckOnLiveDateDestinations() {
        assertTrue(departureInfos.size()>0);

        Set<Station> destinations = departureInfos.stream().map(entry -> entry.getDueTrams().stream()).
                flatMap(Function.identity()).
                map(dueTram -> dueTram.getDestination()).collect(Collectors.toSet());

        Set<String> stationNames = transportData.getStations().stream().map(station -> station.getName()).collect(Collectors.toSet());

        Set<Station> mismatch = destinations.stream().filter(destination -> !stationNames.contains(destination.getName())).
                collect(Collectors.toSet());

        assertTrue(mismatch.toString(), mismatch.isEmpty());
    }

    @Test
    @Category(LiveDataTestCategory.class)
    @Ignore("Part of spike on character set encoding issue for live api")
    public void checkCharacterEncodingOnResponse() throws ParseException {
        String rawJSON = fetcher.fetch();

        JSONParser jsonParser = new JSONParser();
        JSONObject parsed = (JSONObject)jsonParser.parse(rawJSON);
        JSONArray infoList = (JSONArray) parsed.get("value");

        List<String> destinations = new ArrayList<>();
        for (Object item : infoList) {
            JSONObject jsonObject = (JSONObject) item;
            for (int i = 0; i < 4; i++) {
                String place = jsonObject.get(format("Dest%d", i)).toString();
                if (!place.isEmpty()) {
                    destinations.add(place);
                }
            }
        }
        assertFalse(destinations.isEmpty());


    }
}
