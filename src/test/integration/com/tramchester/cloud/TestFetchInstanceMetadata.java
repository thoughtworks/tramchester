package com.tramchester.cloud;

import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class TestFetchInstanceMetadata {

    private URL url;
    private FetchInstanceMetadata fetcher;

    @Before
    public void beforeEachTestRuns() throws MalformedURLException {
        url = new URL("http://localhost:8080/");
        fetcher = new FetchInstanceMetadata(url);
    }

    @Test
    public void shouldFetchInstanceMetadata() throws Exception {
        StubbedAWSServer server = new StubbedAWSServer();
        server.run("someSimpleMetaData");

        String data = fetcher.getUserData();
        server.stopServer();

        assertThat(data).isEqualTo("someSimpleMetaData");
        assertThat(server.getCalledUrl()).isEqualTo("http://localhost:8080/latest/user-data");
    }

    @Test
    public void shouldReturnEmptyIfNoMetaDataAvailable() {
        String result = fetcher.getUserData();
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

}
