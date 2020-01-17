package com.tramchester.integration.livedata;


import com.tramchester.TestConfig;
import com.tramchester.livedata.LiveDataFileFetcher;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertTrue;

public class LiveDataFileFetcherTest {

    @Test
    public void shouldLoadDataInFile() {
        LiveDataFileFetcher fetcher = new LiveDataFileFetcher(TestConfig.LiveDataExampleFile);
        String data = fetcher.fetch();
        assertTrue(data.length()>0);
    }
}
