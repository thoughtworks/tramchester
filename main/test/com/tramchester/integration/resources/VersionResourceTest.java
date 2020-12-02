package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.presentation.Version;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationAppExtension;
import com.tramchester.integration.IntegrationTramTestConfig;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(DropwizardExtensionsSupport.class)
class VersionResourceTest {

    private static final IntegrationTramTestConfig configuration = new IntegrationTramTestConfig();
    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, configuration);

    private final String endPoint = "version";

    @Test
    void shouldGetVersion() {

        Response responce = IntegrationClient.getApiResponse(appExtension, endPoint);
        assertEquals(200, responce.getStatus());

        Version version = responce.readEntity(Version.class);

        String build = System.getenv("BUILD");
        if (build==null) {
            build = "0";
        }
        Assertions.assertEquals(format("2.%s", build), version.getBuildNumber());
    }

    @Test
    void shouldGetTransportModes() {
        Response responce = IntegrationClient.getApiResponse(appExtension, endPoint+"/modes");
        assertEquals(200, responce.getStatus());

        List<TransportMode> results = responce.readEntity(new GenericType<>() {});

        assertFalse(results.isEmpty());

        Set<GTFSTransportationType> configModes = configuration.getTransportModes();
        List<TransportMode> expected = configModes.stream().map(TransportMode::fromGTFS).collect(Collectors.toList());
        assertEquals(expected, results);
    }

}
