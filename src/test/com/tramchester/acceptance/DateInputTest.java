package com.tramchester.acceptance;

import com.tramchester.acceptance.infra.ProvidesChromeDateInput;
import com.tramchester.acceptance.infra.ProvidesFirefoxDateInput;
import org.joda.time.LocalDate;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DateInputTest {

    @Test
    public void shouldGetFirefoxDateCorrectly() {
        ProvidesFirefoxDateInput providesDateInput = new ProvidesFirefoxDateInput();

        LocalDate date = new LocalDate(2019, 11, 30);
        String result = providesDateInput.createDateInput(date);

        // actual ordering is locale specific, which is needed to support browser running in other locals i.e. on CI box
        assertEquals(8, result.length());
        assertTrue(result.contains("30"));
        assertTrue(result.contains("11"));
        assertTrue(result.contains("2019"));
    }

    @Test
    public void shouldGetChromeDateCorrectly() {
        ProvidesChromeDateInput providesDateInput = new ProvidesChromeDateInput();

        LocalDate date = new LocalDate(2019, 11, 30);
        String result = providesDateInput.createDateInput(date);

        // actual ordering is locale specific, which is needed to support browser running in other locals i.e. on CI box
        assertEquals(6, result.length());
        assertTrue(result.contains("30"));
        assertTrue(result.contains("11"));
        assertFalse(result.contains("2019"));
    }

}
