package com.tramchester.integration.dataimport.postcodes;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.postcodes.PostcodeData;
import com.tramchester.dataimport.postcodes.PostcodeDataImporter;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.tram.TramWithPostcodesEnabled;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TestPostcodes;
import com.tramchester.testSupport.testTags.PostcodeTestCategory;
import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@PostcodeTestCategory
class PostcodeDataImporterTest {

    private static ComponentContainer componentContainer;
    private static PostcodeDataImporter importer;
    private static StationLocations stationLocations;
    private static TramchesterConfig testConfig;
    private Set<PostcodeData> loadedPostcodes;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new TramWithPostcodesEnabled(); /// <= means tram stations only, needed for area bounds
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        importer = componentContainer.get(PostcodeDataImporter.class);
        stationLocations = componentContainer.get(StationLocations.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        loadedPostcodes = new HashSet<>();
        importer.loadLocalPostcodes().stream().
                filter(PostcodeDataImporter.PostcodeDataStream::wasLoaded).
                flatMap(PostcodeDataImporter.PostcodeDataStream::getDataStream).
                forEach(postcodeData -> loadedPostcodes.add(postcodeData));
    }

    @AfterEach
    void afterEachTest() {
        loadedPostcodes.clear();
    }

    @Test
    void shouldLoadLocalPostcodesFromFilesInLocation() {

        assertFalse(loadedPostcodes.isEmpty());

        Set<String> postcodes = loadedPostcodes.stream().map(PostcodeData::getId).collect(Collectors.toSet());

        assertFalse(postcodes.contains("EC1A1XH")); // in london, outside area
        assertTrue(postcodes.contains("WA141EP"));
        assertTrue(postcodes.contains("M44BF")); // central manchester
        assertTrue(postcodes.contains(TestPostcodes.postcodeForWythenshaweHosp()));

        // outside stations box but within margin and within range of a station
        assertTrue(postcodes.contains("WA142RQ"));
        assertTrue(postcodes.contains("OL161JZ"));
        assertTrue(postcodes.contains("WA158EW"));
    }

    @Test
    void shouldMatchStationsBounds() {

        // Check bounding box formed by stations plus margin
        long margin = Math.round(testConfig.getNearestStopForWalkingRangeKM() * 1000D);

        BoundingBox bounds = stationLocations.getActiveStationBounds();

        long eastingsMax = loadedPostcodes.stream().
                map(data -> data.getGridPosition().getEastings()).max(Long::compareTo).orElse(-1L);
        long eastingsMin = loadedPostcodes.stream().
                map(data -> data.getGridPosition().getEastings()).min(Long::compareTo).orElse(-1L);

        assertTrue(eastingsMax <= bounds.getMaxEasting()+margin);
        assertTrue(eastingsMin >= bounds.getMinEastings()-margin);

        long northingsMax = loadedPostcodes.stream().
                map(data -> data.getGridPosition().getNorthings()).max(Long::compareTo).orElse(-1L);
        long northingsMin = loadedPostcodes.stream().
                map(data -> data.getGridPosition().getNorthings()).min(Long::compareTo).orElse(-1L);

        assertTrue(northingsMax <= bounds.getMaxNorthings()+margin);
        assertTrue(northingsMin >= bounds.getMinNorthings()-margin,
                northingsMin + " " + bounds.getMinNorthings() + " " +margin);

    }

    @Test
    void shouldOnlyAddPostcodesWithDistanceOfStation() {
        Set<PostcodeData> postcodesOutsideRangeOfAStation = loadedPostcodes.stream().
                filter(this::outsideStationRange).
                collect(Collectors.toSet());
        assertFalse(loadedPostcodes.isEmpty());
        assertTrue(postcodesOutsideRangeOfAStation.isEmpty(), postcodesOutsideRangeOfAStation.toString());
    }

    private boolean outsideStationRange(PostcodeData postcode) {
        return !stationLocations.anyStationsWithinRangeOf(postcode.getGridPosition(),
                MarginInMeters.of(testConfig.getNearestStopForWalkingRangeKM()));
//        List<Station> found = stationLocations.nearestStationsSorted(postcode.getGridPosition(), 1,
//                MarginInMeters.of(testConfig.getNearestStopForWalkingRangeKM()));
//        return found.isEmpty();
    }
}
