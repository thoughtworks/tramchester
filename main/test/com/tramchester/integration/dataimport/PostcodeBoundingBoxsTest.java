package com.tramchester.integration.dataimport;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.PostcodeBoundingBoxs;
import com.tramchester.dataimport.data.PostcodeData;
import com.tramchester.geo.BoundingBox;
import com.tramchester.testSupport.TramWithPostcodesEnabled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

class PostcodeBoundingBoxsTest {

    private final  Path testFolder = Path.of("data", "test", "postcodeTest");
    private Path hintsFiles;
    private PostcodeBoundingBoxs postcodeBoundingBoxs;

    @BeforeEach
    void beforeEachTest() throws IOException {
        TramchesterConfig config = new TramWithPostcodesEnabledTestData();
        hintsFiles = config.getPostcodeDataPath().resolve("postcode_hints.csv");
        postcodeBoundingBoxs = new PostcodeBoundingBoxs(config);

        if (!Files.exists(testFolder)) {
            Files.createDirectory(testFolder);
        }
        Files.deleteIfExists(hintsFiles);
    }

    @AfterEach
    void afterEachTest() throws IOException {
        Files.deleteIfExists(hintsFiles);
    }

    @Test
    void shouldPersitBoundsForPostcodesInFile() throws IOException {

        postcodeBoundingBoxs.start();

        assertFalse(postcodeBoundingBoxs.hasData());

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

        postcodeBoundingBoxs.stop();
        assertTrue(Files.exists(hintsFiles));

        // now should be playback
        postcodeBoundingBoxs.start();
        assertTrue(postcodeBoundingBoxs.hasData());

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
        Files.deleteIfExists(hintsFiles);
        postcodeBoundingBoxs.stop();

        assertFalse(Files.exists(hintsFiles));
    }

    @Test
    void shouldIgnoreZerosForPostcodesInFile() {
        postcodeBoundingBoxs.start();
        Path path = Path.of("anotherFile.csv");
        postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("aaa", 100, 120));
        postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("bbb", 200, 220));
        postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("ccc", 0, 120));
        postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("ddd", 100, 0));
        postcodeBoundingBoxs.checkOrRecord(path, new PostcodeData("eee", 0, 0));

        postcodeBoundingBoxs.stop();
        assertTrue(Files.exists(hintsFiles));

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

    private class TramWithPostcodesEnabledTestData extends TramWithPostcodesEnabled {

        @Override
        public Path getPostcodeDataPath() {
            return testFolder;
        }
    }
}
