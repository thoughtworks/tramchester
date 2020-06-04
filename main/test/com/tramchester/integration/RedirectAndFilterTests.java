package com.tramchester.integration;

import com.tramchester.App;
import com.tramchester.RedirectToHttpsUsingELBProtoHeader;
import com.tramchester.RedirectToAppFilter;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RedirectAndFilterTests {
    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());
    private URL base;
    private URL app;

    @Before
    public void onceBeforeEachTestRuns() throws MalformedURLException {
        base = new URL("http://localhost:" + testRule.getLocalPort());
        app = new URL("http://localhost:" + testRule.getLocalPort() + "/app");
    }

    @Test
    public void shouldUnsecureRedirectToAppIfNoHeaderFromELB() throws IOException {
        HttpURLConnection connection = getConnection(base);
        connection.connect();
        int code = connection.getResponseCode();
        String location = connection.getHeaderField("Location");
        connection.disconnect();

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, code);
        assertTrue(location.startsWith("http://"));
        assertTrue(location.endsWith("/app"));
    }

    @Test
    public void shouldSecureRedirect() throws IOException {
        HttpURLConnection connection = getConnection(base);
        connection.setRequestProperty(RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO, "http");
        connection.connect();
        int code = connection.getResponseCode();
        String location = connection.getHeaderField("Location");
        connection.disconnect();

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, code);
        assertTrue(location.startsWith("https://"));
        assertTrue(location.endsWith("/"));
    }

    @Test
    public void shouldSecureRedirectToAppWithHTTPS() throws IOException {
        HttpURLConnection connection = getConnection(base);
        connection.setRequestProperty(RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO, "https");
        connection.connect();

        String location = connection.getHeaderField("Location");
        int code = connection.getResponseCode();
        connection.disconnect();

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, code);
        assertTrue(location.startsWith("https://"));
        assertTrue(location.endsWith("/app"));
    }

    @Test
    public void shouldNotRedirectTheELBHealthCheck() throws IOException {
        HttpURLConnection connection = getConnection(base);
        connection.setRequestProperty("User-Agent",RedirectToAppFilter.ELB_HEALTH_CHECKER);
        connection.connect();

        int code = connection.getResponseCode();
        connection.disconnect();

        assertEquals(HttpStatus.SC_OK, code);
    }

    @Test
    public void shouldHaveNoRedirectionIfAppPresentAndSecure() throws IOException {
        HttpURLConnection connection = getConnection(app);
        connection.setRequestProperty(RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO, "https");
        connection.connect();

        int code = connection.getResponseCode();
        connection.disconnect();

        assertEquals(HttpStatus.SC_OK, code);
    }

    @Test
    public void shouldRedirectionIfNotSecure() throws IOException {
        HttpURLConnection connection = getConnection(app);
        connection.setRequestProperty(RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO, "http");
        connection.connect();

        String location = connection.getHeaderField("Location");
        int code = connection.getResponseCode();
        connection.disconnect();

        assertTrue(location.startsWith("https://"));
        assertTrue(location.endsWith("/app"));

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, code);
    }

    @NotNull
    private HttpURLConnection getConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(false);
        return connection;
    }

}
