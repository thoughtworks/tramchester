package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.CalendarData;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class CalendarDataMapper extends CSVEntryMapper<CalendarData> {
    private static final Logger logger = LoggerFactory.getLogger(CalendarDataMapper.class);
    private int indexOfServiceId = -1;
    private int indexOfMonday = -1;
    private int indexOfTuesday = -1;
    private int indexOfWednesday = -1;
    private int indexOfThursday = -1;
    private int indexOfFriday = -1;
    private int indexOfSaturday = -1;
    private int indexOfSunday = -1;
    private int indexOfStartDate = -1;
    private int indexOfEndDate = -1;

    private enum Columns implements ColumnDefination {
        service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date
    }

    private final Set<String> serviceIds;
    private final boolean includeAll;

    public CalendarDataMapper(Set<String> serviceIds) {
        this.serviceIds = serviceIds;
        includeAll = serviceIds.isEmpty();
    }

    @Override
    public void initColumnIndex(List<String> headers) {
        indexOfServiceId = findIndexOf(headers, Columns.service_id);
        indexOfMonday = findIndexOf(headers, Columns.monday);
        indexOfTuesday = findIndexOf(headers, Columns.tuesday);
        indexOfWednesday = findIndexOf(headers, Columns.wednesday);
        indexOfThursday = findIndexOf(headers, Columns.thursday);
        indexOfFriday = findIndexOf(headers, Columns.friday);
        indexOfSaturday = findIndexOf(headers, Columns.saturday);
        indexOfSunday = findIndexOf(headers, Columns.sunday);
        indexOfStartDate = findIndexOf(headers, Columns.start_date);
        indexOfEndDate = findIndexOf(headers, Columns.end_date);
    }

    public CalendarData parseEntry(CSVRecord data) {
        String serviceId = data.get(indexOfServiceId);
        boolean monday = data.get(indexOfMonday).equals("1");
        boolean tuesday = data.get(indexOfTuesday).equals("1");
        boolean wednesday = data.get(indexOfWednesday).equals("1");
        boolean thursday = data.get(indexOfThursday).equals("1");
        boolean friday = data.get(indexOfFriday).equals("1");
        boolean saturday = data.get(indexOfSaturday).equals("1");
        boolean sunday = data.get(indexOfSunday).equals("1");
        LocalDate start = parseDate(data.get(indexOfStartDate), LocalDate.MIN, logger);
        LocalDate end = parseDate(data.get(indexOfEndDate), LocalDate.MAX, logger);
        return new CalendarData(serviceId, monday, tuesday, wednesday, thursday, friday, saturday, sunday, start, end);
    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        if (includeAll) {
            return true;
        }
        return serviceIds.contains(data.get(0));
    }

    @Override
    protected ColumnDefination[] getColumns() {
        return Columns.values();
    }
}
