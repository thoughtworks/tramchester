package com.tramchester.resources;


import com.tramchester.*;
import com.tramchester.domain.FeedInfo;
import io.dropwizard.testing.ConfigOverride;
import org.assertj.core.internal.cglib.core.Local;
import org.joda.time.LocalDate;
import org.junit.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class FeedInfoResourceTest {
    public static LocalDate validFrom = new LocalDate(2016, 9, 14);
    public static LocalDate validUntil = new LocalDate(2016,11,14);

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
        assertEquals("20160914", result.getVersion());
        assertEquals(validFrom, result.validFrom());
        assertEquals(validUntil, result.validUntil());
    }

}
