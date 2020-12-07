package com.tramchester.integration.testSupport;

import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class IntegrationClient {

    private final Invocation.Builder builder;

    private IntegrationClient(IntegrationAppExtension appExtension, String endPoint) {
        Client client = appExtension.client();
        WebTarget target = client.target("http://localhost:" + appExtension.getLocalPort() + "/api/" + endPoint);
        builder = target.request(MediaType.APPLICATION_JSON);
        builder.property(ClientProperties.READ_TIMEOUT, 10*1000);
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


    private void setLastMod(Date currentLastMod) {
        DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        builder.header("If-Modified-Since", format.format(currentLastMod));
    }

    public static Response getApiResponse(IntegrationAppExtension appExtension, String endPoint) {
        IntegrationClient integrationClient = new IntegrationClient(appExtension, endPoint);
        return integrationClient.invoke();
    }

    public static Response getApiResponse(IntegrationAppExtension appExtension, String endPoint, Date lastMod) {
        IntegrationClient integrationClient = new IntegrationClient(appExtension, endPoint);
        integrationClient.setLastMod(lastMod);
        return integrationClient.invoke();
    }

}
