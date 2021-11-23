package com.tramchester.dataimport.rail.records;

import com.tramchester.domain.time.TramTime;

public interface RailLocationRecord {
    String getTiplocCode();
    TramTime getPublicArrival();
    TramTime getPublicDeparture();
    String getPlatform();
    boolean hasCallingTimes();
}
