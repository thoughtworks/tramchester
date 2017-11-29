package com.tramchester.integration.livedata;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.livedata.LiveDataFetcher;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.fail;

public class LiveDataFetcherTest {

    @Test
    @Ignore("TODO")
    public void shouldFetchDataFromTFGMAPI() throws URISyntaxException, IOException, TramchesterException {
        LiveDataFetcher liveDataFetcher = new LiveDataFetcher(new IntegrationTramTestConfig());

        String payload = liveDataFetcher.fetch();

        fail("todo");

    }
}
