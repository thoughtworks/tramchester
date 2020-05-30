package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.dataimport.parsers.CalendarDateDataMapper;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class CalendarDatesParserTest {

    @Test
    public void shouldParseData() throws IOException {
        String example = "Serv000001,20200831,2";

        CalendarDateDataMapper mapper = new CalendarDateDataMapper();

        CalendarDateData result = mapper.parseEntry(ParserBuilder.getRecordFor(example));

        assertEquals(result.getServiceId(), "Serv000001");
        assertEquals(LocalDate.of(2020, 8, 31), result.getDate());
        assertEquals(2, result.getExceptionType());

    }
}
