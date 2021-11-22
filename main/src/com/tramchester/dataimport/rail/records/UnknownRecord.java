package com.tramchester.dataimport.rail.records;

import com.tramchester.dataimport.rail.RailRecordType;

public class UnknownRecord implements RailTimetableRecord {
    public UnknownRecord(String line) {

    }

    @Override
    public RailRecordType getRecordType() {
        return RailRecordType.Unknown;
    }
}
