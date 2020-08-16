package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.presentation.Version;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationAppExtension;
import com.tramchester.integration.IntegrationTramTestConfig;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(DropwizardExtensionsSupport.class)
class VersionResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, new IntegrationTramTestConfig());

    @Test
    void shouldGetVersion() {
        String endPoint = "version";

        Response responce = IntegrationClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, responce.getStatus());

        Version version = responce.readEntity(Version.class);

        String build = System.getenv("BUILD");
        if (build==null) {
            build = "0";
        }
        Assertions.assertEquals(format("2.%s", build), version.getBuildNumber());
    }

}
