package com.tramchester.integration.cloud;

import com.tramchester.cloud.FetchInstanceMetadata;
import com.tramchester.integration.IntegrationTramTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;

class FetchInstanceMetadataTest {

    private FetchInstanceMetadata fetcher;

    @BeforeEach
    void beforeEachTestRuns() throws MalformedURLException {
        fetcher = new FetchInstanceMetadata(new IntegrationTramTestConfig());
    }

    @Test
    void shouldFetchInstanceMetadata() throws Exception {
        StubbedAWSServer server = new StubbedAWSServer();
        server.run("someSimpleMetaData");

        String data = fetcher.getUserData();
        server.stopServer();

        assertThat(data).isEqualTo("someSimpleMetaData");
        assertThat(server.getCalledUrl()).isEqualTo("http://localhost:8080/latest/user-data");
    }

    @Test
    void shouldReturnEmptyIfNoMetaDataAvailable() {
        String result = fetcher.getUserData();
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

}
