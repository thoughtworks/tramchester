package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.rail.records.*;
import com.tramchester.domain.time.ProvidesNow;

import javax.inject.Inject;
import java.time.temporal.TemporalField;

@LazySingleton
public class RailDataRecordFactory {

    private final int century;

    @Inject
    public RailDataRecordFactory(ProvidesNow providesNow) {
        century = Math.floorDiv(providesNow.getDate().getYear(), 100);
    }

    public RailTimetableRecord createTIPLOC(String line) {
        return TIPLOCInsert.parse(line);
    }

    public RailTimetableRecord createBasicSchedule(String line) {
        return BasicSchedule.parse(line, century);
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
