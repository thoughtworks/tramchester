package com.tramchester.integration.livedata;


import com.tramchester.livedata.LiveDataFileFetcher;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertTrue;

public class LiveDataFileFetcherTest {

    @Test
    public void shouldLoadDataInFile() {
        Path path = Paths.get("data","test","liveDataSample.json");
        LiveDataFileFetcher fetcher = new LiveDataFileFetcher(path);
        String data = fetcher.fetch();
        assertTrue(data.length()>0);
    }
}
