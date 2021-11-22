package com.tramchester.dataimport.rail.records;

// 1 Record Identity 2 1-2 With the constant value ‘LI’.
// 2 Location 8 3-10 TIPLOC + Suffix.
// 3 Scheduled Arrival Time 5 11-15
// 4 Scheduled Departure Time 5 16-20
// 5 Scheduled Pass   5 21-25
// 6 Public Arrival 4 26-29
// 7 Public Departure 4 30-33
// 8 Platform 3 34-36
// 9 Line 3 37-39
// 10 Path 3 40-42
// 11 Activity 12 43-54
// 12 Engineering Allowance 2 55-56
// 13 Pathing Allowance 2 57-58
// 14 Performance Allowance 2 59-60
// 15 Spare 20 61-80

import com.tramchester.domain.time.TramTime;

public class IntermediateLocation implements RailTimetableRecord {
    private final String tiplocCode;
    private final TramTime publicArrival;
    private final TramTime publicDeparture;
    private final String platform;

    public IntermediateLocation(String tiplocCode, TramTime publicArrival, TramTime publicDeparture, String platform) {
        this.tiplocCode = tiplocCode;
        this.publicArrival = publicArrival;
        this.publicDeparture = publicDeparture;
        this.platform = platform;
    }

    public static IntermediateLocation parse(String line) {
        String tiplocCode = RecordHelper.extract(line, 3, 10+1);
        TramTime publicArrival = RecordHelper.extractTime(line, 25, 28+1);
        TramTime publicDeparture = RecordHelper.extractTime(line, 29, 32+1);
        String platform = RecordHelper.extract(line, 34, 36+1);
        return new IntermediateLocation(tiplocCode, publicArrival, publicDeparture, platform);
    }

    public String getTiplocCode() {
        return tiplocCode;
    }

    public TramTime getPublicArrival() {
        return publicArrival;
    }

    public TramTime getPublicDeparture() {
        return publicDeparture;
    }

    public String getPlatform() {
        return platform;
    }
}
