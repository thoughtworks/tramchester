package com.tramchester.dataimport.rail.records;

import com.tramchester.domain.time.TramTime;

public interface RailLocationRecord extends RailTimetableRecord {
    String getTiplocCode();
    TramTime getArrival();
    TramTime getDeparture();
    String getPlatform();

    boolean isPassingRecord();
}
