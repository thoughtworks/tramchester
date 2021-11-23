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

import com.tramchester.dataimport.rail.RailRecordType;
import com.tramchester.domain.time.TramTime;

public class IntermediateLocation implements RailTimetableRecord, RailLocationRecord {
    private final String tiplocCode;
    private final TramTime publicArrival;
    private final TramTime publicDeparture;
    private final String platform;
    private final boolean hasCallingTimes;

    public IntermediateLocation(String tiplocCode, TramTime publicArrival, TramTime publicDeparture, String platform, boolean hasCallingTimes) {
        this.tiplocCode = tiplocCode;
        this.publicArrival = publicArrival;
        this.publicDeparture = publicDeparture;
        this.platform = platform;
        this.hasCallingTimes = hasCallingTimes;
    }

    public static IntermediateLocation parse(String line) {
        String tiplocCode = RecordHelper.extract(line, 3, 10); // tiploc is 7 long
        TramTime publicArrival = RecordHelper.extractTime(line, 25, 28+1);
        TramTime publicDeparture = RecordHelper.extractTime(line, 29, 32+1);
        String platform = RecordHelper.extract(line, 34, 36+1);
        boolean hasCallingTimes = checkForCallingTimes(line);
        return new IntermediateLocation(tiplocCode, publicArrival, publicDeparture, platform, hasCallingTimes);
    }

    // calling times (non public) are blank for Intermediate Locations where train does not stop
    // Maybe these are signals, junctions, etc?
    private static boolean checkForCallingTimes(String line) {
        String region = line.substring(10, 19);
        return !region.isBlank();
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

    @Override
    public RailRecordType getRecordType() {
        return RailRecordType.IntermediateLocation;
    }

    @Override
    public String toString() {
        return "IntermediateLocation{" +
                "tiplocCode='" + tiplocCode + '\'' +
                ", publicArrival=" + publicArrival +
                ", publicDeparture=" + publicDeparture +
                ", platform='" + platform + '\'' +
                '}';
    }

    public boolean hasCallingTimes() {
        return hasCallingTimes;
    }
}
