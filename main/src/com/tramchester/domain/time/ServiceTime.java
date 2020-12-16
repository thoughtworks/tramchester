package com.tramchester.domain.time;


// TODO just use tramtime

@Deprecated
public class ServiceTime extends TramTime {

    public ServiceTime(int hour, int minutes, int offsetDays) {
        super(hour, minutes, offsetDays);
    }

    @Deprecated
    public static TramTime of(int hours, int minutes) {
        return TramTime.of(hours, minutes);
    }

    @Deprecated
    public static TramTime of(TramTime tramTime) {
        return tramTime;
    }

}
