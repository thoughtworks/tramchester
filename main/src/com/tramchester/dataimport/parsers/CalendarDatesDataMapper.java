package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.CalendarDateData;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class CalendarDatesDataMapper extends  CSVEntryMapper<CalendarDateData> {
    private static final Logger logger = LoggerFactory.getLogger(CalendarDatesDataMapper.class);
    private final Set<String> serviceIds;
    private final boolean includeAll;
    private int indexOfServiceId = -1 ;
    private int indexOfDate = -1 ;
    private int indexOfExceptionType = -1;

    private enum Columns implements ColumnDefination {
        service_id, date, exception_type
    }

    public CalendarDatesDataMapper(Set<String> serviceIds) {
        super();
        this.serviceIds = serviceIds;
        this.includeAll = serviceIds.isEmpty();
    }

    @Override
    public CalendarDateData parseEntry(CSVRecord data) {
        String serviceId = data.get(indexOfServiceId);
        LocalDate date = parseDate(data.get(indexOfDate), LocalDate.MIN, logger);
        int exceptionType = Integer.parseInt(data.get(indexOfExceptionType));
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

    @Override
    public void initColumnIndex(List<String> headers) {
        indexOfServiceId = findIndexOf(headers, Columns.service_id);
        indexOfDate = findIndexOf(headers, Columns.date);
        indexOfExceptionType = findIndexOf(headers, Columns.exception_type);
    }
}
