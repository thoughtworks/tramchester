package com.tramchester.resources;


import com.tramchester.*;
import com.tramchester.domain.FeedInfo;
import io.dropwizard.testing.ConfigOverride;
import org.joda.time.LocalDate;
import org.junit.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class FeedInfoResourceTest {

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    @Test
    public void shouldGetFeedinfoCorrectly() {
        String endPoint = "feedinfo";

        Response responce = IntegrationClient.getResponse(testRule, endPoint);

        FeedInfo result = responce.readEntity(FeedInfo.class);

        assertEquals("Transport for Greater Manchester", result.getPublisherName());
        assertEquals("http://www.tfgm.com", result.getPublisherUrl());
        assertEquals("Europe/London", result.getTimezone());
        assertEquals("en", result.getLang());
        assertEquals("20160809", result.getVersion());
        assertEquals(new LocalDate(2016,8,9), result.validFrom());
        assertEquals(new LocalDate(2016,10,9), result.validUntil());
    }

}
