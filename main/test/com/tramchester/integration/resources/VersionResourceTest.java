package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.presentation.Version;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.util.Optional;

import static java.lang.String.format;

@ExtendWith(DropwizardExtensionsSupport.class)
public class VersionResourceTest {

    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    @Test
    void shouldGetVersion() {
        String endPoint = "version";

        Response responce = IntegrationClient.getApiResponse(testRule, endPoint, Optional.empty(), 200);

        Version version = responce.readEntity(Version.class);

        String build = System.getenv("BUILD");
        if (build==null) {
            build = "0";
        }
        Assertions.assertEquals(format("2.%s", build), version.getBuildNumber());
    }

}
