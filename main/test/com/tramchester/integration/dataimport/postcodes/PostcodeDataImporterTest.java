package com.tramchester.integration.dataimport.postcodes;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.postcodes.PostcodeData;
import com.tramchester.dataimport.postcodes.PostcodeDataImporter;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.tram.TramWithPostcodesEnabled;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostcodeDataImporterTest {

    private static ComponentContainer componentContainer;
    private static PostcodeDataImporter importer;
    private static StationLocations stationLocations;
    private static TramchesterConfig testConfig;
    private Set<PostcodeData> loadedPostcodes;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new TramWithPostcodesEnabled(); /// <= means tram stations only
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
//        importer.loadLocalPostcodes().forEach(source -> {
//            source.getDataStream().forEach(item -> loadedPostcodes.add(item));
//            source.close();
//        });
    }

    @AfterEach
    void afterEachTest() {
        loadedPostcodes.clear();
    }

    @Test
    void shouldLoadLocalPostcodesFromFilesInLocation() {

        assertFalse(loadedPostcodes.isEmpty());

        Set<String> postcodes = loadedPostcodes.stream().map(PostcodeData::getId).collect(Collectors.toSet());

        assertFalse(postcodes.contains("EC1A1XH")); // no london, outside area
        assertTrue(postcodes.contains("WA141EP"));
        assertTrue(postcodes.contains("M44BF")); // central manchester
        assertTrue(postcodes.contains(TestEnv.postcodeForWythenshaweHosp()));

        // outside stations box but within margin and within range of a station
        assertTrue(postcodes.contains("WA142RQ"));
        assertTrue(postcodes.contains("OL161JZ"));
        assertTrue(postcodes.contains("WA144UR"));
    }

    @Test
    void shouldHaveStationsBounds() {

        // Check bounding box formed by stations plus margin
        long margin = Math.round(testConfig.getNearestStopRangeKM() * 1000D);

        BoundingBox bounds = stationLocations.getBounds();

        long eastingsMax = loadedPostcodes.stream().map(data -> data.getGridPosition().getEastings()).max(Long::compareTo).get();
        long eastingsMin = loadedPostcodes.stream().map(data -> data.getGridPosition().getEastings()).min(Long::compareTo).get();

        assertTrue(eastingsMax <= bounds.getMaxEasting()+margin);
        assertTrue(eastingsMin >= bounds.getMinEastings()-margin);

        long northingsMax = loadedPostcodes.stream().map(data -> data.getGridPosition().getNorthings()).max(Long::compareTo).get();
        long northingsMin = loadedPostcodes.stream().map(data -> data.getGridPosition().getNorthings()).min(Long::compareTo).get();

        assertTrue(northingsMax < bounds.getMaxNorthings()+margin);
        assertTrue(northingsMin > bounds.getMinNorthings()-margin);

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
        LatLong latLong = CoordinateTransforms.getLatLong(postcode.getGridPosition());
        List<Station> found = stationLocations.nearestStationsSorted(latLong, 1,
                testConfig.getNearestStopRangeKM());
        return found.isEmpty();
    }
}
