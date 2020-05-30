package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.CalendarData;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Set;

public class CalendarDataMapper extends CSVEntryMapper<CalendarData> {
    private static final Logger logger = LoggerFactory.getLogger(CalendarDataMapper.class);

    private final Set<String> serviceIds;
    private final boolean includeAll;

    public CalendarDataMapper(Set<String> serviceIds) {
        this.serviceIds = serviceIds;
        includeAll = serviceIds.isEmpty();
    }

    public CalendarData parseEntry(CSVRecord data) {
        String serviceId = data.get(0);
        boolean monday = data.get(1).equals("1");
        boolean tuesday = data.get(2).equals("1");
        boolean wednesday = data.get(3).equals("1");
        boolean thursday = data.get(4).equals("1");
        boolean friday = data.get(5).equals("1");
        boolean saturday = data.get(6).equals("1");
        boolean sunday = data.get(7).equals("1");
        LocalDate start = parseDate(data.get(8), LocalDate.MIN, logger);
        LocalDate end = parseDate(data.get(9), LocalDate.MAX, logger);
        return new CalendarData(serviceId, monday, tuesday, wednesday, thursday, friday, saturday, sunday, start, end);
    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        if (includeAll) {
            return true;
        }
        return serviceIds.contains(data.get(0));
    }
}
