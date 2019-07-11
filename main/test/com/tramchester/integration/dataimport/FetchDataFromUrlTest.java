package com.tramchester.integration.dataimport;

import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.TransportDataReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static com.tramchester.Dependencies.TFGM_UNZIP_DIR;
import static junit.framework.TestCase.assertTrue;

public class FetchDataFromUrlTest {

    private Path dataCleansePath;
    private Path unpackedDir;

    @Before
    public void setUp() {
        dataCleansePath = Paths.get("data","testDownload");
        unpackedDir = dataCleansePath.resolve(TFGM_UNZIP_DIR);
    }

    @After
    public void afterEachTestFinishes() throws IOException {
        if (Files.isDirectory(unpackedDir)) {
            List<Path> files = Files.list(unpackedDir).collect(Collectors.toList());
            for (Path file:files) {
                Files.delete(file);
            }
            Files.delete(unpackedDir);
        }
        Files.deleteIfExists(dataCleansePath.resolve(FetchDataFromUrl.ZIP_FILENAME));
        Files.deleteIfExists(dataCleansePath);
    }

    @Test
    public void shouldTestFetchingData() throws IOException {

        FetchDataFromUrl fetcher = new FetchDataFromUrl(dataCleansePath, "http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip");
        fetcher.fetchData();

        assertTrue(Files.isDirectory(unpackedDir));

        assertTrue(Files.isRegularFile(formFilename(TransportDataReader.CALENDAR)));
        assertTrue(Files.isRegularFile(formFilename(TransportDataReader.FEED_INFO)));
        assertTrue(Files.isRegularFile(formFilename(TransportDataReader.ROUTES)));
        assertTrue(Files.isRegularFile(formFilename(TransportDataReader.STOP_TIMES)));
        assertTrue(Files.isRegularFile(formFilename(TransportDataReader.STOPS)));
        assertTrue(Files.isRegularFile(formFilename(TransportDataReader.TRIPS)));
    }

    private Path formFilename(String dataFile) {
        return unpackedDir.resolve(dataFile +".txt");
    }
}
