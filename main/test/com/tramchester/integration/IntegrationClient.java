package com.tramchester.integration;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static org.junit.Assert.assertEquals;


public class IntegrationClient {

    public static Response getApiResponse(IntegrationAppExtension appExtension, String endPoint, Optional<Cookie> cookie, int expectedStatusCode) {
        Client client = appExtension.client();
        WebTarget target = client.target("http://localhost:" + appExtension.getLocalPort() + "/api/"+ endPoint);
        Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON);
        cookie.ifPresent(builder::cookie);
        Response response = builder.get();
        assertEquals(expectedStatusCode, response.getStatus());
        return response;
    }
}
