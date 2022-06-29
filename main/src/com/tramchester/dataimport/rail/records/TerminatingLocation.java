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
import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

public class TerminatingLocation extends OriginOrTerminatingLocation implements RailLocationRecord {
    private static final Logger logger = LoggerFactory.getLogger(TerminatingLocation.class);

    private final String path;

    protected TerminatingLocation(String tiplocCode, TramTime publicDeptTime, String platform, String path, EnumSet<LocationActivityCode> activity) {
        super(tiplocCode, publicDeptTime, platform, activity);
        this.path = path;
    }

    public static TerminatingLocation parse(String text) {
        String path = RecordHelper.extract(text,23, 25+1);
        EnumSet<LocationActivityCode> activity = LocationActivityCode.parse(RecordHelper.extract(text, 26, 37));
        if (activity.isEmpty()) {
            logger.warn("Unknown activity for " + text);
        }
        return OriginOrTerminatingLocation.parse(text, new Creator(path, activity));
    }

    @Override
    public TramTime getArrival() {
        return super.getPublicTime();
    }

    @Override
    public String toString() {
        return "TerminatingLocation{" +
                "path='" + path + '\'' +
                "} " + super.toString();
    }

    @Override
    public TramTime getDeparture() {
        return super.getPublicTime();
    }

    @Override
    public boolean isOrigin() {
        return false;
    }

    @Override
    public boolean isTerminating() {
        return true;
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
        private final EnumSet<LocationActivityCode> activity;

        public Creator(String path, EnumSet<LocationActivityCode> activity) {
            this.path = path;
            this.activity = activity;
        }

        @Override
        public TerminatingLocation create(String tiplocCode, TramTime tramTime, String platform) {
            return new TerminatingLocation(tiplocCode, tramTime, platform, path, activity);
        }
    }

}
