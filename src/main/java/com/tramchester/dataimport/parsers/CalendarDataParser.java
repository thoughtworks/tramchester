package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.dataimport.data.CalendarData;
import org.joda.time.DateTime;

public class CalendarDataParser implements CSVEntryParser<CalendarData> {
    public CalendarData parseEntry(String... data) {
        String serviceId = data[0];
        boolean monday = data[1].equals("1");
        boolean tuesday = data[2].equals("1");
        boolean wednesday = data[3].equals("1");
        boolean thursday = data[4].equals("1");
        boolean friday = data[5].equals("1");
        boolean saturday = data[6].equals("1");
        boolean sunday = data[7].equals("1");
        DateTime start = new DateTime(Integer.parseInt(data[8].substring(0, 4)), Integer.parseInt(data[8].substring(4, 6)), Integer.parseInt(data[8].substring(6, 8)), 0, 0, 0);
        DateTime end = new DateTime(Integer.parseInt(data[9].substring(0, 4)), Integer.parseInt(data[9].substring(4, 6)), Integer.parseInt(data[9].substring(6, 8)), 0, 0, 0);

        return new CalendarData(serviceId, monday, tuesday, wednesday, thursday, friday, saturday, sunday, start, end);
    }
}
