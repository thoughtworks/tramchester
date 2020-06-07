package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.dataimport.parsers.CalendarDatesDataMapper;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarDatesParserTest {
    private final String example = "Serv000001,20200831,2";

    @Test
    void shouldParseData() throws IOException {

        CalendarDatesDataMapper mapper = new CalendarDatesDataMapper(Collections.emptySet());

        CalendarDateData result = mapper.parseEntry(ParserBuilder.getRecordFor(example));

        Assertions.assertEquals(result.getServiceId(), "Serv000001");
        Assertions.assertEquals(LocalDate.of(2020, 8, 31), result.getDate());
        Assertions.assertEquals(2, result.getExceptionType());
    }

    @Test
    void shouldIncludeIfServiceInList() throws IOException {
        CSVRecord recordFor = ParserBuilder.getRecordFor(example);

        CalendarDatesDataMapper calendarDataMapper = new CalendarDatesDataMapper(Collections.emptySet());
        assertThat(calendarDataMapper.shouldInclude(recordFor)).isEqualTo(true);

        Set<String> serviceList = new HashSet<>();
        serviceList.add("Serv000001");
        calendarDataMapper = new CalendarDatesDataMapper(serviceList);
        assertThat(calendarDataMapper.shouldInclude(recordFor)).isEqualTo(true);

        serviceList.clear();
        serviceList.add("ServXXXXX");
        calendarDataMapper = new CalendarDatesDataMapper(serviceList);
        assertThat(calendarDataMapper.shouldInclude(recordFor)).isEqualTo(false);
    }
}
