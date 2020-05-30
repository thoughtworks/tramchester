package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.CalendarDateData;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class CalendarDateDataMapper extends  CSVEntryMapper<CalendarDateData> {
    private static final Logger logger = LoggerFactory.getLogger(CalendarDateDataMapper.class);

    @Override
    public CalendarDateData parseEntry(CSVRecord data) {
        String serviceId = data.get(0);
        LocalDate date = parseDate(data.get(1), LocalDate.MIN, logger);
        int exceptionType = Integer.parseInt(data.get(2));
        return new CalendarDateData(serviceId, date, exceptionType);
    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        return false;
    }
}
