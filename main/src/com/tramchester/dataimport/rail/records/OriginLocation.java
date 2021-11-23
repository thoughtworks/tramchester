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
import com.tramchester.domain.time.TramTime;

public class OriginLocation extends OriginOrTerminatingLocation  implements RailLocationRecord {
    private final String line;

    public OriginLocation(String tiplocCode, TramTime publicDeptTime, String platform, String line) {
        super(tiplocCode, publicDeptTime, platform);
        this.line = line;
    }

    public static OriginLocation parse(String text) {
        String line = RecordHelper.extract(text, 23,25+1);
        return OriginOrTerminatingLocation.parse(text, new Creator(line));
    }

    @Override
    public RailRecordType getRecordType() {
        return RailRecordType.OriginLocation;
    }

    @Override
    public TramTime getPublicArrival() {
        return super.getPublicTime();
    }

    @Override
    public TramTime getPublicDeparture() {
        return super.getPublicTime();
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

        private Creator(String line) {
            this.line = line;
        }

        @Override
        public OriginLocation create(String tiplocCode, TramTime tramTime, String platform) {
            return new OriginLocation(tiplocCode, tramTime, platform, line);
        }
    }
}
