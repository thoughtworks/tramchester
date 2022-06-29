package com.tramchester.unit.domain.time;

import org.junit.jupiter.api.Test;

import static com.tramchester.domain.time.Durations.*;
import static java.time.Duration.ofMinutes;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DurationsTest {
    @Test
    void shouldHaveGreaterThanOrEquals() {
        assertFalse(greaterOrEquals(ofMinutes(1), ofMinutes(2)));
        assertTrue(greaterOrEquals(ofMinutes(2), ofMinutes(2)));
        assertTrue(greaterOrEquals(ofMinutes(3), ofMinutes(2)));
    }

    @Test
    void shouldHaveGreaterThan() {
        // TramTime.difference(arrivalTime, departureTime).compareTo(Duration.ofMinutes(60))>0
        assertFalse(greaterThan(ofMinutes(1), ofMinutes(2)));
        assertFalse(greaterThan(ofMinutes(2), ofMinutes(2)));
        assertTrue(greaterThan(ofMinutes(3), ofMinutes(2)));
    }

    @Test
    void shouldHaveLessThan() {
        assertFalse(lessThan(ofMinutes(2), ofMinutes(1)));
        assertFalse(lessThan(ofMinutes(2), ofMinutes(2)));
        assertTrue(lessThan(ofMinutes(2), ofMinutes(3)));
    }
}
