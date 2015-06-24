package com.tramchester.resource;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationClosureMessage;
import com.tramchester.domain.TransportDataFromFiles;
import com.tramchester.resources.StationResource;
import com.tramchester.services.SpatialService;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.glassfish.jersey.client.ClientResponse;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class StationResourceTest {

    private static final TransportDataFromFiles transportData = new TransportDataBuilder().build();
    private static final SpatialService spatialService = mock(SpatialService.class);
    private static final TramchesterConfig tramchesterConfig = mock(TramchesterConfig.class);


    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new StationResource(transportData, spatialService, tramchesterConfig))
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