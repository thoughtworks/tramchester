package com.tramchester;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;


public class IntegrationClient {

    public static Response getResponse(IntegrationTestRun testRule, String endPoint) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:" + testRule.getLocalPort() + "/api/"+ endPoint);
        Response responce = target.request(MediaType.APPLICATION_JSON).get();
        assertEquals(200, responce.getStatus());
        return responce;
    }
}
