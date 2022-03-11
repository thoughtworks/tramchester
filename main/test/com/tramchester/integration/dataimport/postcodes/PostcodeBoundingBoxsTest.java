package com.tramchester.integration.dataimport.postcodes;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.tramchester.caching.DataCache;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.loader.files.TransportDataFromCSVFile;
import com.tramchester.dataimport.RemoteDataRefreshed;
import com.tramchester.dataimport.data.PostcodeHintData;
import com.tramchester.dataimport.postcodes.PostcodeBoundingBoxs;
import com.tramchester.dataimport.postcodes.PostcodeData;
import com.tramchester.domain.DataSourceID;
import com.tramchester.geo.BoundingBox;
import com.tramchester.integration.testSupport.tram.TramWithPostcodesEnabled;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

class PostcodeBoundingBoxsTest {

    private Path hintsFile;
    private PostcodeBoundingBoxs postcodeBoundingBoxs;
    private CsvMapper mapper;
    private DataCache dataCache;

    @BeforeEach
    void beforeEachTest() {
        mapper = CsvMapper.builder().build();

        TramchesterConfig config = new TramWithPostcodesEnabled();

        hintsFile = config.getCacheFolder().resolve("postcode_hints.csv");

        RemoteDataRefreshed dataRefresh = new RemoteDataRefreshed() {
            @Override
            public boolean refreshed(DataSourceID dataSourceID) {
                return false;
            }

            @Override
            public boolean hasFileFor(DataSourceID dataSourceID) {
                throw new NotImplementedException();
            }

            @Override
            public Path fileFor(DataSourceID dataSourceID) {
                throw new NotImplementedException();
            }
        };

        dataCache = new DataCache(config, dataRefresh, mapper);
        dataCache.start();

        postcodeBoundingBoxs = new PostcodeBoundingBoxs(config, dataCache);

        dataCache.clearFiles();
    }

    @AfterEach
    void afterEachTest() {
        dataCache.clearFiles();
        dataCache.stop();
    }

    @Test
    void shouldMapFilenameToCode() {
        assertEquals("de", postcodeBoundingBoxs.convertPathToCode(Path.of("de.csv")));
        assertEquals("cm", postcodeBoundingBoxs.convertPathToCode(Path.of("data/postcodes/Data/CSV/cm.csv")));
    }

    @Test
    void shouldPersitBoundsForPostcodesInFile() throws IOException {

        postcodeBoundingBoxs.start();

        assertFalse(postcodeBoundingBoxs.isLoaded());

        Path path = Path.of("fileA.csv");
        for (int easting = 10; easting < 20; easting++) {
            for (int northing = 5; northing < 15; northing++) {

                String postcode = format("ZZ%sXX%s", easting, northing);
                PostcodeData postcodeData = new PostcodeData(postcode, easting, northing);
                postcodeBoundingBoxs.checkOrRecord(path, postcodeData);
            }
        }

        for (int easting = 10; easting < 20; easting++) {
            for (int northing = 5; northing < 15; northing++) {
                String postcode = format("ZZ%sXX%s", easting, northing);
                PostcodeData postcodeData = new PostcodeData(postcode, easting, northing);
                assertTrue(postcodeBoundingBoxs.checkOrRecord(path, postcodeData));
            }
        }

        // stop should save the file
        postcodeBoundingBoxs.stop();

        // check file on disc is as expected
        assertTrue(Files.exists(hintsFile), "cache file missing " + hintsFile);
        TransportDataFromCSVFile<PostcodeHintData, PostcodeHintData> loader = new TransportDataFromCSVFile<>(hintsFile, PostcodeHintData.class, mapper);
        List<PostcodeHintData> loadedFromFile = loader.load().collect(Collectors.toList());
        assertEquals(1, loadedFromFile.size());
        PostcodeHintData hintData = loadedFromFile.get(0);
        assertEquals("filea", hintData.getCode());
        assertEquals(10, hintData.getMinEasting());
        assertEquals(19, hintData.getMaxEasting());
        assertEquals(5, hintData.getMinNorthing());
        assertEquals(14, hintData.getMaxNorthing());

        // now should be playback, loads from file
        postcodeBoundingBoxs.start();
        assertTrue(postcodeBoundingBoxs.isLoaded());

        assertFalse(postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("xxx", -11, 0)));
        assertFalse(postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("xxx", 11, 0)));
        assertFalse(postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("xxx", 0, -6)));
        assertFalse(postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("xxx", 0, 6)));

        for (int easting = 10; easting < 20; easting++) {
            for (int northing = 5; northing < 15; northing++) {
                String postcode = format("ZZ%sXX%s", easting, northing);
                PostcodeData postcodeData = new PostcodeData(postcode, easting, northing);
                assertTrue(postcodeBoundingBoxs.checkOrRecord(path, postcodeData), postcodeData.toString());
            }
        }

        BoundingBox results = postcodeBoundingBoxs.getBoundsFor(path);
        assertEquals(10, results.getMinEastings());
        assertEquals(19, results.getMaxEasting());
        assertEquals(5, results.getMinNorthings());
        assertEquals(14, results.getMaxNorthings());

        // delete file, then stop, should not recreate as in playback mode
        Files.deleteIfExists(hintsFile);
        postcodeBoundingBoxs.stop();

        assertFalse(Files.exists(hintsFile));
    }

    @Test
    void shouldIgnoreZerosForPostcodesInFile() {
        postcodeBoundingBoxs.start();
        Path path = Path.of("anotherFile.csv");
        postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("aaa", 100, 120));
        postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("bbb", 200, 220));
        assertFalse(postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("ccc", 0, 120)));
        assertFalse(postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("ddd", 100, 0)));
        assertFalse(postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("eee", 0, 0)));

        postcodeBoundingBoxs.stop();
        assertTrue(Files.exists(hintsFile), "Cache file missing " + hintsFile);

        // now should be playback
        postcodeBoundingBoxs.start();
        assertTrue(postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("rrr", 100, 120)));
        assertTrue(postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("sss", 150, 170)));
        assertTrue(postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("ttt", 200, 220)));

        assertFalse(postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("nnn", 50, 170)));
        assertFalse(postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("nnn", 150, 50)));
        assertFalse(postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("nnn", 50, 50)));
        assertFalse(postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("nnn", 0, 0)));

    }

}
