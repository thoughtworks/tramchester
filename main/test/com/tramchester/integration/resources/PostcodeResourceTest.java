package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.domain.presentation.DTO.PostcodeDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.TramWithPostcodesEnabled;
import com.tramchester.testSupport.reference.TestPostcodes;
import com.tramchester.testSupport.testTags.PostcodeTestCategory;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.tramchester.testSupport.reference.KnownLocations.nearWythenshaweHosp;
import static org.junit.jupiter.api.Assertions.*;

@PostcodeTestCategory
@ExtendWith(DropwizardExtensionsSupport.class)
class PostcodeResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new PostcodesOnlyEnabledResourceConfig());

    private final String endPoint = "postcodes";

    @Test
    void shouldGetLoadedPostcodes() {
        Response response = APIClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, response.getStatus());

        List<PostcodeDTO> results = response.readEntity(new GenericType<>(){});

        assertFalse(results.isEmpty());

        Optional<PostcodeDTO> found = results.stream().
                filter(postcodeDTO -> postcodeDTO.getId().equals(TestPostcodes.postcodeForWythenshaweHosp())).findFirst();
        assertTrue(found.isPresent());

        PostcodeDTO result = found.get();

        //assertEquals("m", result.getArea());
        assertEquals(TestPostcodes.postcodeForWythenshaweHosp(), result.getName());

        LatLong expected = nearWythenshaweHosp.latLong();
        LatLong position = result.getLatLong();
        assertEquals(expected.getLat(), position.getLat(), 0.01);
        assertEquals(expected.getLon(), position.getLon(), 0.01);
    }

    @Test
    void shouldGetTramStation304response() {
        Response resultA = APIClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, resultA.getStatus());

        Date lastMod = resultA.getLastModified();

        Response resultB = APIClient.getApiResponse(appExtension, endPoint, lastMod);
        assertEquals(304, resultB.getStatus());
    }

    private static class PostcodesOnlyEnabledResourceConfig extends TramWithPostcodesEnabled {

        @Override
        public boolean getPlanningEnabled() {
            return false;
        }

        @Override
        public TfgmTramLiveDataConfig getLiveDataConfig() {
            return null;
        }
    }
}
