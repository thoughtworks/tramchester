package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.CalendarData;
import com.tramchester.dataimport.parsers.CalendarDataMapper;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarDataParserTest {
    private final String calendar = "Serv000001,1,1,1,1,1,0,0,20141020,20141219";

    @Test
    void shouldParseCalendarEntry() throws IOException {
        CalendarDataMapper calendarDataMapper = new CalendarDataMapper(Collections.emptySet());

        CSVRecord recordFor = ParserBuilder.getRecordFor(calendar);
        assertThat(calendarDataMapper.shouldInclude(recordFor)).isEqualTo(true);

        CalendarData calendarData = calendarDataMapper.parseEntry(recordFor);
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

    @Test
    void shouldIncludeIfServiceInList() throws IOException {
        CSVRecord recordFor = ParserBuilder.getRecordFor(calendar);

        CalendarDataMapper calendarDataMapper = new CalendarDataMapper(Collections.emptySet());
        assertThat(calendarDataMapper.shouldInclude(recordFor)).isEqualTo(true);

        Set<String> serviceList = new HashSet<>();
        serviceList.add("Serv000001");
        calendarDataMapper = new CalendarDataMapper(serviceList);
        assertThat(calendarDataMapper.shouldInclude(recordFor)).isEqualTo(true);

        serviceList.clear();
        serviceList.add("ServXXXXX");
        calendarDataMapper = new CalendarDataMapper(serviceList);
        assertThat(calendarDataMapper.shouldInclude(recordFor)).isEqualTo(false);
    }
}