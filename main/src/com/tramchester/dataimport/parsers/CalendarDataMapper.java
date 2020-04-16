package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.CalendarData;
import org.apache.commons.csv.CSVRecord;

import java.time.LocalDate;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isNumeric;

public class CalendarDataMapper implements CSVEntryMapper<CalendarData> {

    private final Set<String> serviceIds;
    private final boolean includeAll;

    public CalendarDataMapper(Set<String> serviceIds) {
        this.serviceIds = serviceIds;
        includeAll = serviceIds.isEmpty();
    }

    public CalendarData parseEntry(CSVRecord data) {
        String serviceId = getServiceId(data);
        boolean monday = data.get(1).equals("1");
        boolean tuesday = data.get(2).equals("1");
        boolean wednesday = data.get(3).equals("1");
        boolean thursday = data.get(4).equals("1");
        boolean friday = data.get(5).equals("1");
        boolean saturday = data.get(6).equals("1");
        boolean sunday = data.get(7).equals("1");
        LocalDate start = LocalDate.MIN;
        if (isNumeric(data.get(8))) {
            int year = Integer.parseInt(data.get(8).substring(0, 4));
            int monthOfYear = Integer.parseInt(data.get(8).substring(4, 6));
            int dayOfMonth = Integer.parseInt(data.get(8).substring(6, 8));
            start = LocalDate.of(year, monthOfYear, dayOfMonth);
        }
        LocalDate end = LocalDate.MAX;
        if (isNumeric(data.get(9))) {
            int year = Integer.parseInt(data.get(9).substring(0, 4));
            int monthOfYear = Integer.parseInt(data.get(9).substring(4, 6));
            int dayOfMonth = Integer.parseInt(data.get(9).substring(6, 8));
            end = LocalDate.of(year, monthOfYear, dayOfMonth);
        }
        return new CalendarData(serviceId, monday, tuesday, wednesday, thursday, friday, saturday, sunday, start, end);
    }

    private String getServiceId(CSVRecord data) {
        return data.get(0);
    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        if (includeAll) {
            return true;
        }
        return serviceIds.contains(getServiceId(data));
    }
}
