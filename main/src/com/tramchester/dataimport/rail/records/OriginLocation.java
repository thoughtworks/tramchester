package com.tramchester.dataimport.rail.records;

// 1 Record Identity 2 1-2 With the constant value ‘LO’.
// 2 Location 8 3-10 TIPLOC + Suffix.
// 3 Scheduled Departure Time 5 11-15
// 4 Public Departure Time 4 16-19
// 5 Platform 3 20-22
// 6 Line 3 23-25
// 7 Engineering Allowance 2 26-27
// 8 Pathing Allowance 2 28-29
// 9 Activity 12 30-41
// 10 Performance Allowance 2 42-43
// 11 Spare 37 44-80

import com.tramchester.dataimport.rail.RailRecordType;
import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.domain.time.TramTime;

public class OriginLocation extends OriginOrTerminatingLocation  implements RailLocationRecord {
    private final String line;

    public OriginLocation(String tiplocCode, TramTime publicDeptTime, String platform, String line, LocationActivityCode activity) {
        super(tiplocCode, publicDeptTime, platform, activity);
        this.line = line;
    }

    public static OriginLocation parse(String text) {
        String line = RecordHelper.extract(text, 23,25+1);
        LocationActivityCode activity = LocationActivityCode.parse(RecordHelper.extract(text, 30, 41));
        return OriginOrTerminatingLocation.parse(text, new Creator(line, activity));
    }

    @Override
    public RailRecordType getRecordType() {
        return RailRecordType.OriginLocation;
    }

    @Override
    public TramTime getArrival() {
        return super.getPublicTime();
    }

    @Override
    public TramTime getDeparture() {
        return super.getPublicTime();
    }

    @Override
    public boolean isOrigin() {
        return true;
    }

    @Override
    public boolean isTerminating() {
        return false;
    }

    @Override
    public boolean isPassingRecord() {
        return false;
    }

    @Override
    public String toString() {
        return "OriginLocation{" +
                "line='" + line + '\'' +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        OriginLocation that = (OriginLocation) o;

        return line.equals(that.line);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + line.hashCode();
        return result;
    }

    public String getLine() {
        return line;
    }

    private static class Creator implements Constructor<OriginLocation> {
        private final String line;
        private final LocationActivityCode activity;

        private Creator(String line, LocationActivityCode activity) {
            this.line = line;
            this.activity = activity;
        }

        @Override
        public OriginLocation create(String tiplocCode, TramTime tramTime, String platform) {
            return new OriginLocation(tiplocCode, tramTime, platform, line, activity);
        }
    }
}
