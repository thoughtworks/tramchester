package com.tramchester.integration.testSupport;

import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class APIClient {

    private final Invocation.Builder builder;

    private APIClient(IntegrationAppExtension appExtension, String endPoint) {
        Client client = appExtension.client();
        WebTarget target = client.target("http://localhost:" + appExtension.getLocalPort() + "/api/" + endPoint);
        builder = target.request(MediaType.APPLICATION_JSON);
        builder.property(ClientProperties.READ_TIMEOUT, 10*1000);
    }

    private void setCookie(Cookie cookie) {
        builder.cookie(cookie);
    }

    private void setLastMod(Date currentLastMod) {
        DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        builder.header("If-Modified-Since", format.format(currentLastMod));
    }

    private Response get() {
        return builder.get();
    }

    private Response post(Entity<?> entity) {
        return builder.post(entity);
    }

    public static Response getApiResponse(IntegrationAppExtension appExtension, String endPoint) {
        return getApiResponse(appExtension, endPoint, Collections.emptyList());
    }

    public static Response getApiResponse(IntegrationAppExtension appExtension, String endPoint, List<Cookie> cookieList) {
        APIClient APIClient = new APIClient(appExtension, endPoint);
        cookieList.forEach(APIClient::setCookie);
        return APIClient.get();
    }

    public static <T> Response postAPIRequest(IntegrationAppExtension appExtension, String endPoint, T payload, List<Cookie> cookies) {
        APIClient APIClient = new APIClient(appExtension, endPoint);
        cookies.forEach(APIClient::setCookie);
        Entity<T> entity = Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE);
        return APIClient.post(entity);
    }

    public static <T> Response postAPIRequest(IntegrationAppExtension appExtension, String endPoint, T payload) {
        APIClient APIClient = new APIClient(appExtension, endPoint);
        Entity<T> entity = Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE);
        return APIClient.post(entity);
    }

    public static Response getApiResponse(IntegrationAppExtension appExtension, String endPoint, Date lastMod) {
        APIClient APIClient = new APIClient(appExtension, endPoint);
        APIClient.setLastMod(lastMod);
        return APIClient.get();
    }

}
