package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.dataimport.data.CalendarData;

public class CalendarDataParser implements CSVEntryParser<CalendarData> {
    public CalendarData parseEntry(String... data) {
        String serviceId = data[0];
        boolean monday = data[1].equals("1") ? true : false;
        boolean tuesday = data[2].equals("1") ? true : false;
        boolean wednesday = data[3].equals("1") ? true : false;
        boolean thursday = data[4].equals("1") ? true : false;
        boolean friday = data[5].equals("1") ? true : false;
        boolean saturday = data[6].equals("1") ? true : false;
        boolean sunday = data[7].equals("1") ? true : false;

        return new CalendarData(serviceId, monday, tuesday, wednesday, thursday, friday, saturday, sunday);
    }
}
