package com.tramchester.acceptance;

import com.tramchester.acceptance.infra.ProvidesChromeDateInput;
import com.tramchester.acceptance.infra.ProvidesFirefoxDateInput;
import com.tramchester.acceptance.pages.ProvidesDateInput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

class DateInputTest {

    private final ProvidesDateInput firefoxProvider = new ProvidesFirefoxDateInput();
    private final ProvidesDateInput chromeProvider = new ProvidesChromeDateInput();

    @Test
    void shouldGetFirefoxDateCorrectly() {
        LocalDate date = LocalDate.of(2019, 11, 30);
        String result = firefoxProvider.createDateInput(date);

        // UK is30112019
        Assertions.assertEquals(8, result.length());
        Assertions.assertTrue(result.contains("30"));
        Assertions.assertTrue(result.contains("11"));
        Assertions.assertTrue(result.contains("2019"));
    }

    @Test
    void shouldFirefoxTimeCorrecly() {
        String result = firefoxProvider.createTimeFormat(LocalTime.of(14,45));
        Assertions.assertTrue(result.startsWith("14"), result);
        Assertions.assertTrue(result.endsWith("45"), result);
    }

    @Test
    void shouldGetChromeDateCorrectly() {
        LocalDate date = LocalDate.of(2019, 11, 30);
        String result = chromeProvider.createDateInput(date);

        // actual ordering is locale specific, which is needed to support browser running in other locals i.e. on CI box
        Assertions.assertEquals(8, result.length(), result);
        Assertions.assertTrue(result.contains("30"));
        Assertions.assertTrue(result.contains("11"));
        Assertions.assertTrue(result.contains("2019"));
    }

    @Test
    void shouldGetChromeTimeCorrectly() {
        String result = firefoxProvider.createTimeFormat(LocalTime.of(16,55));
        Assertions.assertTrue(result.startsWith("16"), result);
        Assertions.assertTrue(result.endsWith("55"), result);
    }


}
