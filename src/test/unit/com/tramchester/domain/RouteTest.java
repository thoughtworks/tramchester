package com.tramchester.domain;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class RouteTest {

    @Test
    public void shouldHaveTramRoute() {
        Route route = new Route("id","code","name","MET");
        assertTrue(route.isTram());

        route = new Route("id","code","name","GMS");
        assertFalse(route.isTram());

    }
}
