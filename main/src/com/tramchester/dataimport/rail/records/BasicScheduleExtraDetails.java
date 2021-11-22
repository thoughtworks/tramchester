package com.tramchester.dataimport.rail.records;

public abstract class BasicScheduleExtraDetails implements RailTimetableRecord {
    // 1 Record Identity 2 1-2 With the constant value ‘BX’.
    // 2 Traction Class 4 3-6 Not used - always blank.
    // 3 UIC Code 5 7-11 Only populated for trains travelling to/from Europe via the Channel Tunnel, otherwise blank.
    // 4 ATOC Code 2 12-13
    // 5 Applicable Timetable Code 1 14-14
    // 6 Retail Service ID 8 15-22
    // 7 Source 1 23-23 Not used – always blank.
    // 8 Spare 57 24-80
}
