package com.tramchester.unit.domain;

import com.tramchester.domain.ServiceTimeLimits;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ServiceTimeLimitsTest {

    private ServiceTimeLimits timeLimits = new ServiceTimeLimits();

    @Test
    void shouldHaveCorrectTimeRanges() {
        assertTrue(timeLimits.within(LocalTime.of(7,30)));
        assertTrue(timeLimits.within(LocalTime.of(23,30)));
        assertTrue(timeLimits.within(LocalTime.of(12,30)));

        assertFalse(timeLimits.within(LocalTime.MIDNIGHT));
        assertFalse(timeLimits.within(LocalTime.of(23,55)));
        assertFalse(timeLimits.within(LocalTime.of(1,30)));
    }
}
