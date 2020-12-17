package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.CalendarDateData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

class CalendarDatesParserTest extends ParserTestHelper<CalendarDateData> {
    private final String example = "Serv000001,20200831,2";

    @BeforeEach
    void beforeEach() {
        super.before(CalendarDateData.class, "service_id,date,exception_type");
    }

    @Test
    void shouldParseData() {

        CalendarDateData result = parse(example);

        Assertions.assertEquals(result.getServiceId().forDTO(), "Serv000001");
        Assertions.assertEquals(LocalDate.of(2020, 8, 31), result.getDate());
        Assertions.assertEquals(2, result.getExceptionType());
    }

}
