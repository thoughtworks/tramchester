package com.tramchester.dataimport.rail;

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

    RailRecordType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
