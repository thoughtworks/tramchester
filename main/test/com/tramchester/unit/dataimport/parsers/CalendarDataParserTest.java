package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.CalendarData;
import com.tramchester.dataimport.parsers.CalendarDataParser;
import org.junit.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class CalendarDataParserTest {
    private String calendar = "Serv000001,1,1,1,1,1,0,0,20141020,20141219";

    @Test
    public void shouldParseTrip() throws Exception {
        CalendarDataParser calendarParserTest = new CalendarDataParser();
        CalendarData calendarData = calendarParserTest.parseEntry(this.calendar.split(","));

        assertThat(calendarData.getServiceId()).isEqualTo("Serv000001");
        assertThat(calendarData.isMonday()).isEqualTo(true);
        assertThat(calendarData.isTuesday()).isEqualTo(true);
        assertThat(calendarData.isWednesday()).isEqualTo(true);
        assertThat(calendarData.isThursday()).isEqualTo(true);
        assertThat(calendarData.isFriday()).isEqualTo(true);
        assertThat(calendarData.isSaturday()).isEqualTo(false);
        assertThat(calendarData.isSunday()).isEqualTo(false);
        assertThat(calendarData.getStartDate()).isEqualTo(LocalDate.of(2014, 10, 20));
        assertThat(calendarData.getEndDate()).isEqualTo(LocalDate.of(2014, 12, 19));
    }
}