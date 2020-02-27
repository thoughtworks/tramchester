package com.tramchester.integration.livedata;


import com.tramchester.testSupport.TestConfig;
import com.tramchester.livedata.LiveDataFileFetcher;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class LiveDataFileFetcherTest {

    @Test
    public void shouldLoadDataInFile() {
        LiveDataFileFetcher fetcher = new LiveDataFileFetcher(TestConfig.LiveDataExampleFile);
        String data = fetcher.fetch();
        assertTrue(data.length()>0);
    }
}
