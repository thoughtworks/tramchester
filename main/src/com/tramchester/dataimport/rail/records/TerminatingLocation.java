package com.tramchester.dataimport.rail.records;

// 1 Record Identity 2 1-2 With the constant value ‘LT’.
// 2 Location 8 3-10 TIPLOC +Suffix.
// 3 Scheduled Arrival Time 5 11-15
// 4 Public Arrival Time 4 16-19
// 5 Platform 3 20-22
// 6 Path 3 23-25
// 7 Activity 12 26-37
// 8 Spare 43 38-80

import com.tramchester.domain.time.TramTime;

public class TerminatingLocation extends OriginOrTerminatingLocation implements RailTimetableRecord {

    protected TerminatingLocation(String tiplocCode, TramTime publicDeptTime, String platform) {
        super(tiplocCode, publicDeptTime, platform);
    }

    public static TerminatingLocation parse(String line) {
        return OriginOrTerminatingLocation.parse(line, TerminatingLocation::new);
    }

    public TramTime getPublicArrivalTime() {
        return super.getPublicTime();
    }
}
