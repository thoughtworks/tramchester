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


public class APIClient {

    private final Invocation.Builder builder;

    private APIClient(IntegrationAppExtension appExtension, String endPoint) {
        Client client = appExtension.client();
        WebTarget target = client.target("http://localhost:" + appExtension.getLocalPort() + "/api/" + endPoint);
        builder = target.request(MediaType.APPLICATION_JSON);
        builder.property(ClientProperties.READ_TIMEOUT, 10*1000);
    }

    private Response invoke() {
        return builder.get();
    }

    public static Response getApiResponse(IntegrationAppExtension appExtension, String endPoint, Cookie cookie) {
        APIClient APIClient = new APIClient(appExtension, endPoint);
        APIClient.setCookie(cookie);
        return APIClient.invoke();
    }

    private void setCookie(Cookie cookie) {
        builder.cookie(cookie);
    }


    private void setLastMod(Date currentLastMod) {
        DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        builder.header("If-Modified-Since", format.format(currentLastMod));
    }

    public static Response getApiResponse(IntegrationAppExtension appExtension, String endPoint) {
        APIClient APIClient = new APIClient(appExtension, endPoint);
        return APIClient.invoke();
    }

    public static Response getApiResponse(IntegrationAppExtension appExtension, String endPoint, Date lastMod) {
        APIClient APIClient = new APIClient(appExtension, endPoint);
        APIClient.setLastMod(lastMod);
        return APIClient.invoke();
    }

}
