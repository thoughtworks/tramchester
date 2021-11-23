package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.rail.records.*;
import com.tramchester.domain.time.ProvidesNow;

import javax.inject.Inject;

@LazySingleton
public class RailDataRecordFactory {

    private final ProvidesNow providesNow;

    @Inject
    public RailDataRecordFactory(ProvidesNow providesNow) {
        this.providesNow = providesNow;
    }

    public RailTimetableRecord createTIPLOC(String line) {
        return TIPLOCInsert.parse(line);
    }

    public RailTimetableRecord createBasicSchedule(String line) {
        return BasicSchedule.parse(line, providesNow);
    }

    public RailTimetableRecord createOrigin(String line) {
        return OriginLocation.parse(line);
    }

    public IntermediateLocation createIntermediate(String line) {
        return IntermediateLocation.parse(line);
    }

    public TerminatingLocation createTerminating(String line) {
        return TerminatingLocation.parse(line);
    }

    public RailTimetableRecord createBasicScheduleExtraDetails(String line) {
        return BasicScheduleExtraDetails.parse(line);
    }
}
