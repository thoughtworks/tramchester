package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.CalendarData;
import org.joda.time.DateTime;
import org.junit.Test;

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
        assertThat(calendarData.getStartDate()).isEqualTo(new DateTime(2014, 10, 20, 0, 0));
        assertThat(calendarData.getEndDate()).isEqualTo(new DateTime(2014, 12, 19, 0, 0));
    }
}