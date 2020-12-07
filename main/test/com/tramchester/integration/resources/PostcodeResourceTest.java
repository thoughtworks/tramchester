package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.PostcodeDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.testSupport.IntegrationClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.integration.testSupport.TramWithPostcodesEnabled;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class PostcodeResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new TramWithPostcodesEnabled());
    private final String endPoint = "postcodes";

    @Test
    void shouldGetLoadedPostcodes() {
        Response response = IntegrationClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, response.getStatus());

        List<PostcodeDTO> results = response.readEntity(new GenericType<>(){});

        assertFalse(results.isEmpty());

        Optional<PostcodeDTO> found = results.stream().
                filter(postcodeDTO -> postcodeDTO.getId().equals(TestEnv.postcodeForWythenshaweHosp())).findFirst();
        assertTrue(found.isPresent());

        PostcodeDTO result = found.get();

        assertEquals("M23", result.getArea());
        assertEquals(TestEnv.postcodeForWythenshaweHosp(), result.getName());


        LatLong expected = TestEnv.nearWythenshaweHosp();
        LatLong position = result.getLatLong();
        assertEquals(expected.getLat(), position.getLat(), 0.01);
        assertEquals(expected.getLon(), position.getLon(), 0.01);
    }

    @Test
    void shouldGetTramStation304response() {
        Response resultA = IntegrationClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, resultA.getStatus());

        Date lastMod = resultA.getLastModified();

        Response resultB = IntegrationClient.getApiResponse(appExtension, endPoint, lastMod);
        assertEquals(304, resultB.getStatus());
    }

}
