package com.tramchester.unit.dataimport.rail;

import com.tramchester.dataimport.rail.records.RecordHelper;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordHelperTest {


    @Test
    void shouldParseTime() {
        String text = "xxxx1145yyyy";
        TramTime result = RecordHelper.extractTime(text, 4, 8);

        assertEquals(TramTime.of(11,45), result);
    }

    @Test
    void shouldParseDate() {
        String text = "xxxx220513yyyyy";

        ProvidesNow providesNow = new ProvidesLocalNow();

        LocalDate result = RecordHelper.extractDate(text, 5, 11, providesNow);

        assertEquals(LocalDate.of(2022, 5, 13), result);
    }

    @Test
    void shouldExtractText() {
        String text = "ABCD12345vwxyz";

        assertEquals("ABCD", RecordHelper.extract(text, 1, 5));

        assertEquals("12345", RecordHelper.extract(text, 5, 10));

        assertEquals("vwxyz", RecordHelper.extract(text, 10, 15));

    }


}
