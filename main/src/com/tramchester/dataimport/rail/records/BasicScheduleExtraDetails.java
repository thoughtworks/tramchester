package com.tramchester.dataimport.rail.records;

// 1 Record Identity 2 1-2 With the constant value ‘BX’.
// 2 Traction Class 4 3-6 Not used - always blank.
// 3 UIC Code 5 7-11 Only populated for trains travelling to/from Europe via the Channel Tunnel, otherwise blank.
// 4 ATOC Code 2 12-13
// 5 Applicable Timetable Code 1 14-14
// 6 Retail Service ID 8 15-22
// 7 Source 1 23-23 Not used – always blank.
// 8 Spare 57 24-80

import com.tramchester.dataimport.rail.RailRecordType;

public class BasicScheduleExtraDetails implements RailTimetableRecord {

    private final String atocCode;
    private final String retailServiceID;

    public BasicScheduleExtraDetails(String atocCode, String retailServiceID) {
        this.atocCode = atocCode;
        this.retailServiceID = retailServiceID;
    }

    public static BasicScheduleExtraDetails parse(String line) {
        String atocCode = RecordHelper.extract(line, 12, 13+1);
        String retailServiceId = RecordHelper.extract(line, 15, 22+1);
        return new BasicScheduleExtraDetails(atocCode, retailServiceId);
    }

    public String getAtocCode() {
        return atocCode;
    }

    public String getRetailServiceID() {
        return retailServiceID;
    }

    @Override
    public RailRecordType getRecordType() {
        return RailRecordType.BasicScheduleExtra;
    }
}
