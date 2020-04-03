package com.tramchester.integration.livedata;


import com.tramchester.livedata.LiveDataFileFetcher;
import com.tramchester.testSupport.TestEnv;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class LiveDataFileFetcherTest {

    @Test
    public void shouldLoadDataInFile() {
        LiveDataFileFetcher fetcher = new LiveDataFileFetcher(TestEnv.LiveDataExampleFile);
        String data = fetcher.fetch();
        assertTrue(data.length()>0);
    }
}
