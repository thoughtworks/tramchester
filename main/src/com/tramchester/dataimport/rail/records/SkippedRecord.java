package com.tramchester.dataimport.rail.records;

import com.tramchester.dataimport.rail.RailRecordType;

public class SkippedRecord implements RailTimetableRecord {
    public SkippedRecord(RailRecordType recordType, String line) {

    }

    @Override
    public RailRecordType getRecordType() {
        return RailRecordType.Skipped;
    }
}
