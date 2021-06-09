package com.tramchester.integration.livedata;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.Lines;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.livedata.LiveDataHTTPFetcher;
import com.tramchester.mappers.LiveDataParser;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.testTags.LiveDataTestCategory;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

class LiveDataHTTPFetcherTest {

    private static ComponentContainer componentContainer;
    private static LiveDataHTTPFetcher fetcher;
    private static String payload;
    private static IntegrationTramTestConfig configuration;

    private TransportData transportData;
    private LiveDataParser parser;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        configuration = new IntegrationTramTestConfig(true);
        componentContainer = new ComponentsBuilder().create(configuration, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        // don't want to fetch every time
        fetcher = componentContainer.get(LiveDataHTTPFetcher.class);
        payload = fetcher.fetch();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = componentContainer.get(TransportData.class);
        parser = componentContainer.get(LiveDataParser.class);
    }

    @Test
    void shouldHaveTFGMKeyInConfig() {
        assertNotNull(configuration.getLiveDataConfig().getDataSubscriptionKey(), "missing tfgm live data key");
    }

    @Test
    @LiveDataTestCategory
    void shouldFetchSomethingFromTFGM() {
        assertNotNull(payload);
        assertFalse(payload.isEmpty());
    }

    @Test
    @LiveDataTestCategory
    void shouldFetchValidDataFromTFGMAPI() {
        List<StationDepartureInfo> departureInfos = parser.parse(payload);

        assertTrue(departureInfos.size()>0);

        Optional<StationDepartureInfo> hasMsgs = departureInfos.stream().
                filter(info -> !info.getMessage().isEmpty()).findAny();

        assertTrue(hasMsgs.isPresent(), "display with msgs");

        StationDepartureInfo display = hasMsgs.get();

        // this assert will fail if run at certain times of day....
        // assertTrue(aDisplay.getDueTrams().size()>0);
        assertNotEquals(Lines.UnknownLine, display.getLine());
        LocalDateTime when = display.getLastUpdate();
        Assertions.assertEquals(TestEnv.LocalNow().getDayOfMonth(),when.getDayOfMonth());
    }

    @Test
    @LiveDataTestCategory
    void shouldHaveCrosscheckOnLiveDateDestinations() {
        List<StationDepartureInfo> departureInfos = parser.parse(payload);

        assertTrue(departureInfos.size()>0);

        Set<Station> destinations = departureInfos.stream().flatMap(entry -> entry.getDueTrams().stream()).
                map(DueTram::getDestination).collect(Collectors.toSet());

        Set<String> stationNames = transportData.getStations().stream().map(Station::getName).collect(Collectors.toSet());

        Set<Station> mismatch = destinations.stream().filter(destination -> !stationNames.contains(destination.getName())).
                collect(Collectors.toSet());

        assertTrue(mismatch.isEmpty(), mismatch.toString());
    }

    @Test
    @LiveDataTestCategory
    @Disabled("Part of spike on character set encoding issue for live api")
    void checkCharacterEncodingOnResponse()  {
        String rawJSON = fetcher.fetch();

        //JSONParser jsonParser = new JSONParser();
        JsonObject parsed = Jsoner.deserialize(rawJSON, new JsonObject());
        assertTrue(parsed.containsKey("value"));
        JsonArray infoList = (JsonArray) parsed.get("value");

        List<String> destinations = new ArrayList<>();
        for (Object item : infoList) {
            JsonObject jsonObject = (JsonObject) item;
            for (int i = 0; i < 4; i++) {
                String place = jsonObject.get(format("Dest%d", i)).toString();
                if (!place.isEmpty()) {
                    destinations.add(place);
                }
            }
        }
        assertFalse(destinations.isEmpty());
    }

    @Test
    @LiveDataTestCategory
    void shouldMapAllLinesCorrectly() {
        List<StationDepartureInfo> departureInfos = parser.parse(payload);

        Set<Lines> uniqueLines = departureInfos.stream().map(StationDepartureInfo::getLine).collect(Collectors.toSet());

        assertFalse(uniqueLines.contains(Lines.UnknownLine));

        assertEquals(8, uniqueLines.size());
    }

    @Test
    @LiveDataTestCategory
    void shouldSpikeDisplayDirectionsForRoutes() {
        List<StationDepartureInfo> departureInfos = parser.parse(payload);

        Set<Station> stations = transportData.getStations();

        stations.forEach(station -> {
            Set<Lines> lines = getLineAndDirectionFor(departureInfos, station);
            System.out.println(station.getName() + " " + lines.toString());
        });
    }

    @Test
    void shouldHaveRealStationNamesForMappings() {
        List<LiveDataParser.LiveDataNamesMapping> mappings = Arrays.asList(LiveDataParser.LiveDataNamesMapping.values());
        mappings.forEach(mapping -> assertTrue(transportData.getTramStationByName(mapping.getToo()).isPresent(), mapping.name()));
    }

    private Set<Lines> getLineAndDirectionFor(List<StationDepartureInfo> departureInfos, Station station) {
        return departureInfos.stream().filter(departureInfo -> departureInfo.getStation().equals(station)).
                map(StationDepartureInfo::getLine).collect(Collectors.toSet());
    }


}
