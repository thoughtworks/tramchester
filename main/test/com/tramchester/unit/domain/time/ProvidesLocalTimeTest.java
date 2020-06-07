package com.tramchester.unit.domain.time;

import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.testSupport.TestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class ProvidesLocalTimeTest {

    @Test
    void shouldProvideTimeWithCorrectZone() {
        ProvidesLocalNow providesLocalNow = new ProvidesLocalNow();

        LocalDateTime dateTime = providesLocalNow.getDateTime();
        // not great
        Assertions.assertEquals(dateTime.atZone(TestConfig.TimeZone).toLocalDateTime(), dateTime);

    }
}
