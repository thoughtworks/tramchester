package com.tramchester.integration.dataimport.postcodes;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.DataLoader;
import com.tramchester.dataimport.postcodes.PostcodeBoundingBoxs;
import com.tramchester.dataimport.postcodes.PostcodeData;
import com.tramchester.dataimport.data.PostcodeHintData;
import com.tramchester.geo.BoundingBox;
import com.tramchester.integration.testSupport.tram.TramWithPostcodesEnabled;
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

    private final  Path testFolder = Path.of("data", "test", "postcodeTest");
    private Path hintsFile;
    private PostcodeBoundingBoxs postcodeBoundingBoxs;
    private CsvMapper mapper;

    @BeforeEach
    void beforeEachTest() throws IOException {
        mapper = CsvMapper.builder().build();

        TramchesterConfig config = new TramWithPostcodesEnabled();
        RemoteDataSourceConfig sourceConfig = config.getDataSourceConfig("postcodes");
        hintsFile = sourceConfig.getDataPath().resolve("postcode_hints.csv");
        postcodeBoundingBoxs = new PostcodeBoundingBoxs(config, mapper);

        if (!Files.exists(testFolder)) {
            Files.createDirectory(testFolder);
        }
        Files.deleteIfExists(hintsFile);
    }

    @AfterEach
    void afterEachTest() throws IOException {
        Files.deleteIfExists(hintsFile);
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
        assertTrue(Files.exists(hintsFile));
        DataLoader<PostcodeHintData> loader = new DataLoader<>(hintsFile, PostcodeHintData.class, mapper);
        List<PostcodeHintData> loadedFromFile = loader.load().collect(Collectors.toList());
        assertEquals(1, loadedFromFile.size());
        PostcodeHintData hintData = loadedFromFile.get(0);
        assertEquals("fileA.csv", hintData.getFile());
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
        assertTrue(Files.exists(hintsFile));

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
