package com.tramchester.dataimport.rail;

import com.google.inject.ImplementedBy;
import com.tramchester.dataimport.rail.records.RailTimetableRecord;

import java.util.stream.Stream;

@ImplementedBy(LoadRailTimetableRecords.class)
public interface ProvidesRailTimetableRecords {
    Stream<RailTimetableRecord> load();
}
