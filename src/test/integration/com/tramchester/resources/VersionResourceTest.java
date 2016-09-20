package com.tramchester.resources;

import com.tramchester.App;
import com.tramchester.IntegrationClient;
import com.tramchester.IntegrationTestRun;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.domain.Version;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.util.Optional;

import static org.junit.Assert.assertEquals;


public class VersionResourceTest {
    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    @Test
    public void shouldGetVersion() {
        String endPoint = "version";

        Response responce = IntegrationClient.getResponse(testRule, endPoint, Optional.empty());

        Version version = responce.readEntity(Version.class);

        String build = System.getenv("BUILD");
        if (build==null) {
            build = "0";
        }
        assertEquals(build, version.getBuildNumber());
    }

}
