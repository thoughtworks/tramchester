package com.tramchester.resources;


import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.domain.FeedInfo;
import org.joda.time.LocalDate;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class FeedInfoResourceTest {
    private static Dependencies dependencies;
    private FeedInfoResource feedInfoResource;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        feedInfoResource = dependencies.get(FeedInfoResource.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldGetFeedinfoCorrectly() {
        Response responce = feedInfoResource.get();
        FeedInfo result = (FeedInfo) responce.getEntity();

        assertEquals("Transport for Greater Manchester", result.getPublisherName());
        assertEquals("http://www.tfgm.com", result.getPublisherUrl());
        assertEquals("Europe/London", result.getTimezone());
        assertEquals("en", result.getLang());
        assertEquals("20160809", result.getVersion());
        assertEquals(new LocalDate(2016,8,9), result.validFrom());
        assertEquals(new LocalDate(2016,10,9), result.validUntil());

    }


}
