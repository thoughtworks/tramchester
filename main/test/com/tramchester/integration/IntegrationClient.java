package com.tramchester.integration;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


public class IntegrationClient {

    private final Invocation.Builder builder;

    private IntegrationClient(IntegrationAppExtension appExtension, String endPoint) {
        Client client = appExtension.client();
        WebTarget target = client.target("http://localhost:" + appExtension.getLocalPort() + "/api/" + endPoint);
        builder = target.request(MediaType.APPLICATION_JSON);
    }

    private Response invoke() {
        return builder.get();
    }

    public static Response getApiResponse(IntegrationAppExtension appExtension, String endPoint, Cookie cookie) {
        IntegrationClient integrationClient = new IntegrationClient(appExtension, endPoint);
        integrationClient.setCookie(cookie);
        return integrationClient.invoke();
    }

    private void setCookie(Cookie cookie) {
        builder.cookie(cookie);
    }

    public static Response getApiResponse(IntegrationAppExtension appExtension, String endPoint) {
        IntegrationClient integrationClient = new IntegrationClient(appExtension, endPoint);
        return integrationClient.invoke();
    }
}
