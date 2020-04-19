package com.tramchester.integration.dataimport;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.PostcodeDataImporter;
import com.tramchester.dataimport.data.PostcodeData;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.integration.IntegrationTramTestConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PostcodeDataImporterTest {

    private static Dependencies dependencies;
    private static PostcodeDataImporter importer;
    private static StationLocations stationLocations;
    private static IntegrationTramTestConfig testConfig;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
        importer = dependencies.get(PostcodeDataImporter.class);
        stationLocations = dependencies.get(StationLocations.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldLoadLocalPostcodesFromFilesInLocation() {

        Set<PostcodeData> results = importer.loadLocalPostcodes();

        assertFalse(results.isEmpty());

        Set<String> postcodes = results.stream().map(PostcodeData::getId).collect(Collectors.toSet());

        assertTrue(postcodes.contains("M44BF")); // central manchester
        assertFalse(postcodes.contains("EC1A1XH")); // no london, outside area

        // outside stations box but within margin
        assertTrue(postcodes.contains("WA142RQ"));
        assertTrue(postcodes.contains("OL161JZ"));
        assertTrue(postcodes.contains("WA144UR"));

        // Check bounding box formed by stations plus margin
        long margin = Math.round(testConfig.getNearestStopRangeKM() * 1000D);

        int eastingsMax = results.stream().map(PostcodeData::getEastings).max(Integer::compareTo).get();
        int eastingsMin = results.stream().map(PostcodeData::getEastings).min(Integer::compareTo).get();
        assertTrue(eastingsMax <= stationLocations.getEastingsMax()+margin);
        assertTrue(eastingsMin >= stationLocations.getEastingsMin()-margin);

        int northingsMax = results.stream().map(PostcodeData::getNorthings).max(Integer::compareTo).get();
        int northingsMin = results.stream().map(PostcodeData::getNorthings).min(Integer::compareTo).get();
        assertTrue(northingsMax < stationLocations.getNorthingsMax()+margin);
        assertTrue(northingsMin > stationLocations.getNorthingsMin()-margin);

        results.clear();
    }
}
