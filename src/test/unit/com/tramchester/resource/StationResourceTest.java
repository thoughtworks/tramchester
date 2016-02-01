package com.tramchester.resource;

import com.tramchester.TestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ClosedStations;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.resources.StationResource;
import com.tramchester.services.SpatialService;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


public class StationResourceTest {

    private static final TransportDataFromFiles transportData = new TransportDataBuilder().build();
    private static final SpatialService spatialService = mock(SpatialService.class);
    private static final List<String> closedStations = asList("St Peters Square");

    private static TramchesterConfig testConfig = new TestConfig() {
        @Override
        public Path getDataFolder() {
            return null;
        }

        @Override
        public String getGraphName() {
            return null;
        }

        @Override
        public Set<String> getAgencies() {
            return null;
        }

        @Override
        public boolean useGenericMapper() {
            return false;
        }
    };

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new StationResource(transportData, spatialService,
                    new ClosedStations(closedStations), testConfig))
            .build();

    @Test
    public void shouldGetAllStops() throws Exception {

        Response response = resources.client()
                .target("/stations")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        assertThat(response.getStatus()).isEqualTo(200);

    }

    @Test
    public void shouldFailIfStopNotExists() throws Exception {

        Response response = resources.client()
                .target("/stations/123")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        assertThat(response.getStatus()).isEqualTo(404);

    }

}