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

// https://wiki.openraildata.com/index.php?title=CIF_Schedule_Records

import com.tramchester.dataimport.rail.RailRecordType;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntermediateLocation implements RailLocationRecord {
    private static final Logger logger = LoggerFactory.getLogger(IntermediateLocation.class);
    public static final TramTime missingPublicTime = TramTime.of(0, 0);

    private final String tiplocCode;
    private final TramTime publicArrival;
    private final TramTime publicDeparture;
    private final TramTime passingTime;
    private final String platform;
    private final TramTime scheduledArrival;
    private final TramTime scheduledDepart;
    private final TramTime blankTime = TramTime.of(0,0); // 0000 is in some of the data records

    public IntermediateLocation(String tiplocCode, TramTime scheduledArrival, TramTime scheduledDepart, TramTime publicArrival,
                                TramTime publicDeparture, String platform,
                                TramTime passingTime) {
        this.tiplocCode = tiplocCode;
        this.scheduledArrival = scheduledArrival;
        this.scheduledDepart = scheduledDepart;
        this.publicArrival = publicArrival;
        this.publicDeparture = publicDeparture;
        this.platform = platform;
        this.passingTime = passingTime;
    }

    public static IntermediateLocation parse(String text) {
        String tiplocCode = RecordHelper.extract(text, 3, 10); // tiploc is 7 long
        TramTime scheduledArrival = RecordHelper.extractTime(text, 10,14);
        TramTime scheduledDepart = RecordHelper.extractTime(text, 15, 19);
        TramTime passingTime = RecordHelper.extractTime(text, 20, 23+1);
        boolean isPassing = passingTime.isValid();
        TramTime publicArrival = getPublicTime(text, isPassing, 25, 28 + 1);
        TramTime publicDeparture = getPublicTime(text, isPassing, 29, 32+1);
        String platform = RecordHelper.extract(text, 34, 36+1);
        return new IntermediateLocation(tiplocCode, scheduledArrival, scheduledDepart, publicArrival, publicDeparture, platform, passingTime);
    }

    private static TramTime getPublicTime(String text, boolean isPassing, int begin, int end) {
        TramTime result = RecordHelper.extractTime(text, begin, end);
        if (isPassing) {
            if (!result.equals(missingPublicTime)) {
                logger.warn("Passing is set but valid public time of " + result);
            }
            return TramTime.invalid();
        }
        return result;
    }

    public String getTiplocCode() {
        return tiplocCode;
    }

    @Override
    public TramTime getArrival() {
        return pickPublicOrSchedule(publicArrival, scheduledArrival);
    }

    @Override
    public TramTime getDeparture() {
        return pickPublicOrSchedule(publicDeparture, scheduledDepart);
    }

    private TramTime pickPublicOrSchedule(TramTime pub, TramTime scheduled) {
        if (pub.isValid()) {
            if (scheduled.isValid() && pub.equals(blankTime)) {
                return scheduled;
            }
            return pub;
        }
        return scheduled;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntermediateLocation that = (IntermediateLocation) o;

        if (!tiplocCode.equals(that.tiplocCode)) return false;
        if (!publicArrival.equals(that.publicArrival)) return false;
        if (!publicDeparture.equals(that.publicDeparture)) return false;
        return platform.equals(that.platform);
    }

    @Override
    public int hashCode() {
        int result = tiplocCode.hashCode();
        result = 31 * result + publicArrival.hashCode();
        result = 31 * result + publicDeparture.hashCode();
        result = 31 * result + platform.hashCode();
        return result;
    }

    @Override
    public boolean isPassingRecord() {
        return passingTime.isValid();
    }

    public TramTime getPassingTime() {
        return passingTime;
    }

    public TramTime getScheduledArrival() {
        return scheduledArrival;
    }

    public TramTime getScheduledDeparture() {
        return scheduledDepart;
    }
}
