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

    private final String code;

    private static final HashMap<String, RailRecordType> map = new HashMap<>();

    static {
        for(RailRecordType recordType : RailRecordType.values()) {
            map.put(recordType.code, recordType);
        }
    }

    RailRecordType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static RailRecordType parse(String code) {
        if (map.containsKey(code)) {
            return map.get(code);
        }
        return Unknown;
    }
}
