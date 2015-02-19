package com.tramchester.resources;

import com.tramchester.domain.TransportData;
import com.tramchester.services.SpatialService;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


public class StationResourceTest {

    private static final TransportData transportData = new TransportDataBuilder().build();
    private static final SpatialService spatialService = mock(SpatialService.class);


    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new StationResource(transportData, spatialService))
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