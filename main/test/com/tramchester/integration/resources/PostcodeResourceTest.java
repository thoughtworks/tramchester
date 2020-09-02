package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.PostcodeDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationAppExtension;
import com.tramchester.testSupport.TramWithPostcodesEnabled;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class PostcodeResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new TramWithPostcodesEnabled());

    @Test
    void shouldGetLoadedPostcodes() {
        String endPoint = "postcodes";
        Response response = IntegrationClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, response.getStatus());

        List<PostcodeDTO> results = response.readEntity(new GenericType<>(){});

        assertFalse(results.isEmpty());

        Optional<PostcodeDTO> found = results.stream().
                filter(postcodeDTO -> postcodeDTO.getId().equals("M139WL")).findFirst();
        assertTrue(found.isPresent());

        PostcodeDTO result = found.get();

        assertEquals("M13", result.getArea());
        assertEquals("M139WL", result.getName());

        double lat = 53.4620378;
        double lon = -2.2280871;
        LatLong position = result.getLatLong();
        assertEquals(lat, position.getLat(), 0.001);
        assertEquals(lon, position.getLon(), 0.001);
    }

}
