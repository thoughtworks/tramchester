package com.tramchester.integration.dataimport;

import com.tramchester.Dependencies;
import com.tramchester.dataimport.PostcodeDataImporter;
import com.tramchester.dataimport.data.PostcodeData;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.IntegrationTramTestConfig;
import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PostcodeDataImporterTest {

    private static Dependencies dependencies;
    private static PostcodeDataImporter importer;
    private static StationLocations stationLocations;
    private static IntegrationTramTestConfig testConfig;
    private Set<PostcodeData> loadedPostcodes;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
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
        loadedPostcodes = new HashSet<>();
        importer.loadLocalPostcodes().forEach(source -> {
            source.forEach(item -> loadedPostcodes.add(item));
            source.close();
        });
    }

    @AfterEach
    void afterEachTest() {
        loadedPostcodes.clear();
    }

    @Test
    void shouldLoadLocalPostcodesFromFilesInLocation() {

        assertFalse(loadedPostcodes.isEmpty());

        Set<String> postcodes = loadedPostcodes.stream().map(PostcodeData::getId).collect(Collectors.toSet());

        Assertions.assertTrue(postcodes.contains("M44BF")); // central manchester
        assertFalse(postcodes.contains("EC1A1XH")); // no london, outside area

        // outside stations box but within margin and within range of a station
        Assertions.assertTrue(postcodes.contains("WA142RQ"));
        Assertions.assertTrue(postcodes.contains("OL161JZ"));
        Assertions.assertTrue(postcodes.contains("WA144UR"));

        // Check bounding box formed by stations plus margin
        long margin = Math.round(testConfig.getNearestStopRangeKM() * 1000D);

        long eastingsMax = loadedPostcodes.stream().map(PostcodeData::getEastings).max(Long::compareTo).get();
        long eastingsMin = loadedPostcodes.stream().map(PostcodeData::getEastings).min(Long::compareTo).get();

        BoundingBox bounds = stationLocations.getBounds();
        Assertions.assertTrue(eastingsMax <= bounds.getMaxEasting()+margin);
        Assertions.assertTrue(eastingsMin >= bounds.getMinEastings()-margin);

        long northingsMax = loadedPostcodes.stream().map(PostcodeData::getNorthings).max(Long::compareTo).get();
        long northingsMin = loadedPostcodes.stream().map(PostcodeData::getNorthings).min(Long::compareTo).get();
        Assertions.assertTrue(northingsMax < bounds.getMaxNorthings()+margin);
        Assertions.assertTrue(northingsMin > bounds.getMinNorthings()-margin);

        loadedPostcodes.clear();
    }

    @Test
    void shouldOnlyAddPostcodesWithDistanceOfStation() {
        Set<PostcodeData> postcodesOutsideRangeOfAStation = loadedPostcodes.stream().
                filter(this::outsideStationRange).
                collect(Collectors.toSet());
        assertFalse(loadedPostcodes.isEmpty());
        Assertions.assertTrue(postcodesOutsideRangeOfAStation.isEmpty(), postcodesOutsideRangeOfAStation.toString());
    }

    private boolean outsideStationRange(PostcodeData postcode) {
        List<Station> found = stationLocations.nearestStationsSorted(postcode, 1, testConfig.getNearestStopRangeKM());
        return found.isEmpty();
    }
}
