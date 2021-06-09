package com.tramchester.unit.dataimport.data;

import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.unit.dataimport.ParserTestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class CalendarDatesParserTest extends ParserTestHelper<CalendarDateData> {

    @BeforeEach
    void beforeEach() {
        super.before(CalendarDateData.class, "service_id,date,exception_type");
    }

    @Test
    void shouldParseData() {

        String example = "Serv000001,20200831,2";
        CalendarDateData result = parse(example);

        Assertions.assertEquals(result.getServiceId().forDTO(), "Serv000001");
        Assertions.assertEquals(LocalDate.of(2020, 8, 31), result.getDate());
        Assertions.assertEquals(2, result.getExceptionType());
    }

}
