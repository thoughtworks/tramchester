package com.tramchester.dataimport.rail.records;

// 1 Record Identity 2 1-2 With the constant value ‘LT’.
// 2 Location 8 3-10 TIPLOC +Suffix.
// 3 Scheduled Arrival Time 5 11-15
// 4 Public Arrival Time 4 16-19
// 5 Platform 3 20-22
// 6 Path 3 23-25
// 7 Activity 12 26-37
// 8 Spare 43 38-80

import com.tramchester.dataimport.rail.RailRecordType;
import com.tramchester.domain.time.TramTime;

public class TerminatingLocation extends OriginOrTerminatingLocation implements RailLocationRecord {

    private final String path;

    protected TerminatingLocation(String tiplocCode, TramTime publicDeptTime, String platform, String path) {
        super(tiplocCode, publicDeptTime, platform);
        this.path = path;
    }

    public static TerminatingLocation parse(String text) {
        String path = RecordHelper.extract(text,23, 25+1);
        return OriginOrTerminatingLocation.parse(text, new Creator(path));
    }

    @Override
    public TramTime getPublicArrival() {
        return super.getPublicTime();
    }

    @Override
    public String toString() {
        return "TerminatingLocation{" +
                "path='" + path + '\'' +
                "} " + super.toString();
    }

    @Override
    public TramTime getPublicDeparture() {
        return super.getPublicTime();
    }

    @Override
    public boolean isPassingRecord() {
        return false;
    }

    @Override
    public RailRecordType getRecordType() {
        return RailRecordType.TerminatingLocation;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        TerminatingLocation that = (TerminatingLocation) o;

        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }

    private static class Creator implements Constructor<TerminatingLocation> {

        private final String path;

        public Creator(String path) {
            this.path = path;
        }

        @Override
        public TerminatingLocation create(String tiplocCode, TramTime tramTime, String platform) {
            return new TerminatingLocation(tiplocCode, tramTime, platform, path);
        }
    }

}
