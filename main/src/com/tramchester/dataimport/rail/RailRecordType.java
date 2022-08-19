package com.tramchester.dataimport.rail;

import java.util.HashMap;

public enum RailRecordType {
    TiplocInsert("TI"),
    BasicSchedule("BS"),
    BasicScheduleExtra("BX"),
    ChangesEnRoute("CR"),
    IntermediateLocation("LI"),
    OriginLocation("LO"),
    TerminatingLocation("LT"),
    Header("HD"),
    Association("AA"),
    Skipped("SkippedForNow"),
    Trailer("ZZ"),
    Unknown("UnknownCode");

    private final CharSequence code;

    private static final HashMap<CharSequence, RailRecordType> map = new HashMap<>();

    static {
        for(RailRecordType recordType : RailRecordType.values()) {
            map.put(recordType.code, recordType);
        }
    }

    RailRecordType(CharSequence code) {
        this.code = code;
    }

    public static RailRecordType parse(CharSequence code) {
        if (map.containsKey(code)) {
            return map.get(code);
        }
        return Unknown;
    }
}
