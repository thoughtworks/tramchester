package com.tramchester.unit.dataimport.data;

import com.tramchester.dataimport.data.CalendarData;
import com.tramchester.domain.time.DateRange;
import com.tramchester.unit.dataimport.ParserTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarDataParserTest extends ParserTestHelper<CalendarData> {

    @BeforeEach
    void beforeEach() {
        super.before(CalendarData.class, "service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date");
    }

    @Test
    void shouldParseCalendarEntry() {

        String calendar = "Serv000001,1,1,1,1,1,0,0,20141020,20141219";
        CalendarData calendarData = parse(calendar);

        assertThat(calendarData.getServiceId().forDTO()).isEqualTo("Serv000001");
        assertThat(calendarData.isMonday()).isEqualTo(true);
        assertThat(calendarData.isTuesday()).isEqualTo(true);
        assertThat(calendarData.isWednesday()).isEqualTo(true);
        assertThat(calendarData.isThursday()).isEqualTo(true);
        assertThat(calendarData.isFriday()).isEqualTo(true);
        assertThat(calendarData.isSaturday()).isEqualTo(false);
        assertThat(calendarData.isSunday()).isEqualTo(false);
        DateRange dateRange = calendarData.getDateRange();
        assertThat(dateRange.getStartDate()).isEqualTo(LocalDate.of(2014, 10, 20));
        assertThat(dateRange.getEndDate()).isEqualTo(LocalDate.of(2014, 12, 19));
    }


}