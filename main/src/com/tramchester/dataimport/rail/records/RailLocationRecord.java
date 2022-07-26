package com.tramchester.dataimport.rail.records;

import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.domain.time.TramTime;

import java.util.EnumSet;

public interface RailLocationRecord extends RailTimetableRecord {
    String getTiplocCode();
    TramTime getArrival();
    TramTime getDeparture();
    String getPlatform();

    EnumSet<LocationActivityCode> getActivity();

    boolean isOrigin();
    boolean isTerminating();
    boolean doesStop();

    TramTime getPassingTime();
}
