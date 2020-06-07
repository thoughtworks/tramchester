package com.tramchester.integration.dataimport;

import com.tramchester.Dependencies;
import com.tramchester.dataimport.PostcodeDataImporter;
import com.tramchester.dataimport.data.PostcodeData;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.IntegrationTramTestConfig;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class PostcodeDataImporterTest {

    private static Dependencies dependencies;
    private static PostcodeDataImporter importer;
    private static StationLocations stationLocations;
    private static IntegrationTramTestConfig testConfig;
    private Set<PostcodeData> loadedPostcodes;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig(); /// <= means tram stations only
        dependencies.initialise(testConfig);
        importer = dependencies.get(PostcodeDataImporter.class);
        stationLocations = dependencies.get(StationLocations.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        loadedPostcodes = importer.loadLocalPostcodes();
    }

    @Test
    void shouldLoadLocalPostcodesFromFilesInLocation() {

        Assertions.assertFalse(loadedPostcodes.isEmpty());

        Set<String> postcodes = loadedPostcodes.stream().map(PostcodeData::getId).collect(Collectors.toSet());

        Assertions.assertTrue(postcodes.contains("M44BF")); // central manchester
        Assertions.assertFalse(postcodes.contains("EC1A1XH")); // no london, outside area

        // outside stations box but within margin and within range of a station
        Assertions.assertTrue(postcodes.contains("WA142RQ"));
        Assertions.assertTrue(postcodes.contains("OL161JZ"));
        Assertions.assertTrue(postcodes.contains("WA144UR"));

        // Check bounding box formed by stations plus margin
        long margin = Math.round(testConfig.getNearestStopRangeKM() * 1000D);

        int eastingsMax = loadedPostcodes.stream().map(PostcodeData::getEastings).max(Integer::compareTo).get();
        int eastingsMin = loadedPostcodes.stream().map(PostcodeData::getEastings).min(Integer::compareTo).get();
        Assertions.assertTrue(eastingsMax <= stationLocations.getEastingsMax()+margin);
        Assertions.assertTrue(eastingsMin >= stationLocations.getEastingsMin()-margin);

        int northingsMax = loadedPostcodes.stream().map(PostcodeData::getNorthings).max(Integer::compareTo).get();
        int northingsMin = loadedPostcodes.stream().map(PostcodeData::getNorthings).min(Integer::compareTo).get();
        Assertions.assertTrue(northingsMax < stationLocations.getNorthingsMax()+margin);
        Assertions.assertTrue(northingsMin > stationLocations.getNorthingsMin()-margin);

        loadedPostcodes.clear();
    }

    @Test
    void shouldOnlyAddPostcodesWithDistanceOfStation() {
        Set<PostcodeData> postcodesOutsideRangeOfAStation = loadedPostcodes.stream().
                filter(this::outsideStationRange).
                collect(Collectors.toSet());
        Assertions.assertTrue(postcodesOutsideRangeOfAStation.isEmpty(), postcodesOutsideRangeOfAStation.toString());
    }

    private boolean outsideStationRange(PostcodeData postcode) {
        StationLocations.GridPosition gridPosition = new StationLocations.GridPosition(postcode.getEastings(), postcode.getNorthings());
        List<Station> found = stationLocations.nearestStations(gridPosition, 1, testConfig.getNearestStopRangeKM());
        return found.isEmpty();
    }
}
