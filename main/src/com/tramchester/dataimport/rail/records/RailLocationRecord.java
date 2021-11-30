package com.tramchester.dataimport.rail.records;

import com.tramchester.domain.time.TramTime;

public interface RailLocationRecord extends RailTimetableRecord {
    String getTiplocCode();
    TramTime getPublicArrival();
    TramTime getPublicDeparture();
    String getPlatform();

    boolean isPassingRecord();
}
