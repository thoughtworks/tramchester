package com.tramchester.integration.cloud;

import com.tramchester.cloud.FetchInstanceMetadata;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;

class FetchInstanceMetadataTest {

    @Test
    void shouldFetchInstanceMetadata() throws Exception {

        FetchInstanceMetadata fetcher = new FetchInstanceMetadata(new ConfigWithMetaDataUrl("http://localhost:8080"));

        StubbedAWSServer server = new StubbedAWSServer();
        server.run("someSimpleMetaData");

        String data = fetcher.getUserData();
        server.stopServer();

        assertThat(data).isEqualTo("someSimpleMetaData");
        assertThat(server.getCalledUrl()).isEqualTo("http://localhost:8080/latest/user-data");
    }

    @Test
    void shouldReturnEmptyIfNoMetaDataAvailable() {
        FetchInstanceMetadata fetcher = new FetchInstanceMetadata(new ConfigWithMetaDataUrl("http://localhost:8080"));

        String result = fetcher.getUserData();
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyIfNoUrl() {
        FetchInstanceMetadata fetcher = new FetchInstanceMetadata(new ConfigWithMetaDataUrl(""));

        String result = fetcher.getUserData();
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }


    private static class ConfigWithMetaDataUrl extends IntegrationTramTestConfig {

        private final String url;

        private ConfigWithMetaDataUrl(String url) {
            this.url = url;
        }

        @Override
        public String getInstanceDataUrl() {
            return url;
        }
    }
}
