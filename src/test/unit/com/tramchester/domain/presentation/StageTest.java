package com.tramchester.domain.presentation;


import org.junit.Test;

import java.time.LocalTime;
import java.util.Arrays;

import static junit.framework.TestCase.assertEquals;

public class StageTest {

    @Test
    public void shouldGetDurationCorrectly() {
        Stage stage = new Stage("firstStation", "route", "Tram", "cssClass");
        ServiceTime serviceTime = new ServiceTime(LocalTime.of(8, 00), LocalTime.of(9, 15), "svcId", "headsign", 30);
        stage.setServiceTimes(Arrays.asList(serviceTime));

        assertEquals(75, stage.getDuration());
    }

    @Test
    public void shouldGetDurationCorrectlyWhenAfterMidnight() {
        Stage stage = new Stage("firstStation", "route", "Tram", "cssClass");
        ServiceTime serviceTime = new ServiceTime(LocalTime.of(23, 50), LocalTime.of(0, 15), "svcId", "headsign", 30);
        stage.setServiceTimes(Arrays.asList(serviceTime));

        assertEquals(25, stage.getDuration());
    }

    @Test
    public void shouldGetRouteCode() {
        Stage stage = new Stage("firstStation", "route", "Tram", "cssClass");
        assertEquals("cssClass", stage.getTramRouteId());
    }

    @Test
    public void shouldGetMode() {
        Stage stage = new Stage("firstStation", "route", "Tram", "cssClass");
        assertEquals("Tram", stage.getMode());
    }
}
