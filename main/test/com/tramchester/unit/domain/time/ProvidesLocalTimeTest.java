package com.tramchester.unit.domain.time;

import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.testSupport.TestConfig;
import org.junit.Test;

import java.time.LocalDateTime;

import static junit.framework.TestCase.assertEquals;

public class ProvidesLocalTimeTest {

    @Test
    public void shouldProvideTimeWithCorrectZone() {
        ProvidesLocalNow providesLocalNow = new ProvidesLocalNow();

        LocalDateTime dateTime = providesLocalNow.getDateTime();
        // not great
        assertEquals(dateTime.atZone(TestConfig.TimeZone).toLocalDateTime(), dateTime);

    }
}
