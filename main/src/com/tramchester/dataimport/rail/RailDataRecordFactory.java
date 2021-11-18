package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.dataimport.rail.records.TIPLOCInsert;

public class RailDataRecordFactory {

    public RailTimetableRecord createTIPLOC(String line) {
        return TIPLOCInsert.parse(line);
    }
}
