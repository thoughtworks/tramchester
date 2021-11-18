package com.tramchester.dataimport.rail.records;

public class ChangesEnRoute implements RailTimetableRecord {
    // 1 Record Identity 2 1-2 With the constant value ‘CR’.
    // 2 Location 8 3-10 TIPLOC + Suffix.
    // 3 Train Category 2 11-12
    // 4 Train Identity 4 13-16
    // 5 Headcode 4 17-20
    // 6 Course Indicator 1 21-21
    // 7 Profit Centre Code/ Train Service Code 22-29
    // 8 Business Sector 1 30-30
    // 9 Power Type 3 31-33
    // 10 Timing Load 4 34-37
    // 11 Speed 3 38-40
    // 12 Operating Chars 6 41-46
    // 13 Train Class 1 47-47
    // 14 Sleepers 1 48-48
    // 15 Reservations 1 49-49
    // 16 Connect Indicator 1 50-50
    // 17 Catering Code 4 51-54
    // 18 Service Branding 4 55-58
    // 19 Traction Class 4 59-62
    // 20 UIC Code 5 63-67 Only populated for trains travelling to/from Europe via the Channel Tunnel, otherwise blank.
}
