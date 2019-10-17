package com.tramchester.integration.dataimport;

import com.googlecode.jcsv.CSVStrategy;
import com.googlecode.jcsv.reader.CSVReader;
import com.googlecode.jcsv.reader.internal.CSVReaderBuilder;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.parsers.FeedInfoDataParser;
import com.tramchester.domain.FeedInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.tramchester.Dependencies.TFGM_UNZIP_DIR;
import static java.lang.String.format;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

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
        Path zipPath = dataCleansePath.resolve(FetchDataFromUrl.ZIP_FILENAME);
        if (Files.exists(zipPath)) {
            Files.deleteIfExists(zipPath);
        }

        try {
            Files.deleteIfExists(dataCleansePath);
        }
        catch (DirectoryNotEmptyException exception) {
           // workaround for windows
        }
    }

    @Test
    public void shouldGetStreamForFeedInfoFileFromInMemoryZip() throws IOException {
        FetchDataFromUrl fetcher = new FetchDataFromUrl(dataCleansePath, "http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip");

        ByteArrayInputStream inputStream = fetcher.streamForSingleFile(format("%s/%s.txt", TFGM_UNZIP_DIR, TransportDataReader.FEED_INFO));

        InputStreamReader reader = new InputStreamReader(inputStream);

        CSVStrategy csvStrategy = new CSVStrategy(',', '"', '#', true, true);
        CSVReader<FeedInfo> cvsParser = new CSVReaderBuilder<FeedInfo>(reader).
                entryParser(new FeedInfoDataParser()).strategy(csvStrategy).
                build();
        List<FeedInfo> result = cvsParser.readAll();
        reader.close();
        inputStream.close();

        assertEquals(1,result.size());
        assertEquals("http://www.tfgm.com", result.get(0).getPublisherUrl());
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
