package com.tramchester.unit.domain;

import com.tramchester.livedata.domain.liveUpdates.DueTram;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class DueTramTest {

    @Test
    void calculateWhenCorrectly() {
        LocalTime updateTime = LocalTime.of(10,42);
        DueTram dueTram = new DueTram(TramStations.Bury.fake(), "Due", 4, "Double", updateTime);

        TramTime result = dueTram.getWhen();
        assertEquals(updateTime.plusMinutes(4), result.asLocalTime());
        assertFalse(result.isNextDay());

    }

    @Test
    void calculateWhenCorrectAcrossMidnight() {
        LocalTime updateTime = LocalTime.of(23,58);
        DueTram dueTram = new DueTram(TramStations.Bury.fake(), "Due", 4, "Double", updateTime);

        TramTime result = dueTram.getWhen();
        assertEquals(LocalTime.of(0,2), result.asLocalTime());
        assertTrue(result.isNextDay());
    }
}
