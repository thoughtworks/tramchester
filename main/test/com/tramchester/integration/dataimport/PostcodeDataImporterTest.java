package com.tramchester.integration.dataimport;

import com.tramchester.Dependencies;
import com.tramchester.dataimport.PostcodeDataImporter;
import com.tramchester.dataimport.data.PostcodeData;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.IntegrationTramTestConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PostcodeDataImporterTest {

    private static Dependencies dependencies;
    private static PostcodeDataImporter importer;
    private static StationLocations stationLocations;
    private static IntegrationTramTestConfig testConfig;
    private Set<PostcodeData> loadedPostcodes;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig(); /// <= means tram stations only
        dependencies.initialise(testConfig);
        importer = dependencies.get(PostcodeDataImporter.class);
        stationLocations = dependencies.get(StationLocations.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void onceBeforeEachTestRuns() {
        loadedPostcodes = importer.loadLocalPostcodes();
    }

    @Test
    public void shouldLoadLocalPostcodesFromFilesInLocation() {

        assertFalse(loadedPostcodes.isEmpty());

        Set<String> postcodes = loadedPostcodes.stream().map(PostcodeData::getId).collect(Collectors.toSet());

        assertTrue(postcodes.contains("M44BF")); // central manchester
        assertFalse(postcodes.contains("EC1A1XH")); // no london, outside area

        // outside stations box but within margin and within range of a station
        assertTrue(postcodes.contains("WA142RQ"));
        assertTrue(postcodes.contains("OL161JZ"));
        assertTrue(postcodes.contains("WA144UR"));

        // Check bounding box formed by stations plus margin
        long margin = Math.round(testConfig.getNearestStopRangeKM() * 1000D);

        int eastingsMax = loadedPostcodes.stream().map(PostcodeData::getEastings).max(Integer::compareTo).get();
        int eastingsMin = loadedPostcodes.stream().map(PostcodeData::getEastings).min(Integer::compareTo).get();
        assertTrue(eastingsMax <= stationLocations.getEastingsMax()+margin);
        assertTrue(eastingsMin >= stationLocations.getEastingsMin()-margin);

        int northingsMax = loadedPostcodes.stream().map(PostcodeData::getNorthings).max(Integer::compareTo).get();
        int northingsMin = loadedPostcodes.stream().map(PostcodeData::getNorthings).min(Integer::compareTo).get();
        assertTrue(northingsMax < stationLocations.getNorthingsMax()+margin);
        assertTrue(northingsMin > stationLocations.getNorthingsMin()-margin);

        loadedPostcodes.clear();
    }

    @Test
    public void shouldOnlyAddPostcodesWithDistanceOfStation() {
        Set<PostcodeData> postcodesOutsideRangeOfAStation = loadedPostcodes.stream().
                filter(this::outsideStationRange).
                collect(Collectors.toSet());
        assertTrue(postcodesOutsideRangeOfAStation.toString(), postcodesOutsideRangeOfAStation.isEmpty());
    }

    private boolean outsideStationRange(PostcodeData postcode) {
        StationLocations.GridPosition gridPosition = new StationLocations.GridPosition(postcode.getEastings(), postcode.getNorthings());
        List<Station> found = stationLocations.nearestStations(gridPosition, 1, testConfig.getNearestStopRangeKM());
        return found.isEmpty();
    }
}
