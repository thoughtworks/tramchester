package com.tramchester.domain;

import org.joda.time.DateTime;

public enum DaysOfWeek {
    Sunday, Saturday, Friday, Thursday, Wednesday, Tuesday, Monday;

//    public static DaysOfWeek fromToday() {
//        return fromDate(DateTime.now());
//    }

    public static DaysOfWeek fromDate(DateTime date) {
        int dayOfWeek = date.getDayOfWeek();
        switch (dayOfWeek) {
            case 1:
                return Monday;
            case 2:
                return Tuesday;
            case 3:
                return Wednesday;
            case 4:
                return Thursday;
            case 5:
                return Friday;
            case 6:
                return Saturday;
            case 7:
                return Sunday;
            default:
                return Monday;
        }
    }
}
