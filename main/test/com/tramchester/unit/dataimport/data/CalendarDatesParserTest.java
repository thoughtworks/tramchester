package com.tramchester.unit.dataimport.data;

import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.unit.dataimport.ParserTestCSVHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.tramchester.integration.testSupport.Assertions.assertIdEquals;

class CalendarDatesParserTest extends ParserTestCSVHelper<CalendarDateData> {

    @BeforeEach
    void beforeEach() {
        super.before(CalendarDateData.class, "service_id,date,exception_type");
    }

    @Test
    void shouldParseData() {

        String example = "Serv000001,20200831,2";
        CalendarDateData result = parse(example);

        assertIdEquals("Serv000001", result.getServiceId());
        Assertions.assertEquals(TramDate.of(2020, 8, 31), result.getDate());
        Assertions.assertEquals(2, result.getExceptionType());
    }

}
