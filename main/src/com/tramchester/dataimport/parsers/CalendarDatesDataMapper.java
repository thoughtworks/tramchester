package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.CalendarDateData;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Set;

public class CalendarDatesDataMapper extends  CSVEntryMapper<CalendarDateData> {
    private static final Logger logger = LoggerFactory.getLogger(CalendarDatesDataMapper.class);
    private final Set<String> serviceIds;
    private final boolean includeAll;

    public CalendarDatesDataMapper(Set<String> serviceIds) {
        super();
        this.serviceIds = serviceIds;
        this.includeAll = serviceIds.isEmpty();
    }

    @Override
    public CalendarDateData parseEntry(CSVRecord data) {
        String serviceId = data.get(0);
        LocalDate date = parseDate(data.get(1), LocalDate.MIN, logger);
        int exceptionType = Integer.parseInt(data.get(2));
        if (!(exceptionType==CalendarDateData.ADDED || exceptionType==CalendarDateData.REMOVED)) {
            logger.warn("Unexpected exception type " + exceptionType + " for service " + serviceId);
        }
        return new CalendarDateData(serviceId, date, exceptionType);
    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        if (includeAll) {
            return true;
        }
        return serviceIds.contains(data.get(0));
    }
}
