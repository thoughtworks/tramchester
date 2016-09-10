package com.tramchester.domain;


import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import static java.lang.String.format;

public class TramServiceDate {
    private org.joda.time.LocalDate date;

    public TramServiceDate(LocalDate date) {
        this.date = date;
    }

    public TramServiceDate(String date) {
        int year = Integer.parseInt(date.substring(0, 4));
        int monthOfYear = Integer.parseInt(date.substring(4, 6));
        int dayOfMonth = Integer.parseInt(date.substring(6, 8));
        this.date = new LocalDate(year, monthOfYear, dayOfMonth);
    }

    public LocalDate getDate() {
        return date;
    }

    public String getStringDate() {
        return date.toString("yyyyMMdd");
    }

    @Override
    public String toString() {
        return "TramServiceDate{" +
                "date=" + date +
                ", day=" + date.getDayOfWeek() +
                '}';
    }

    public DaysOfWeek getDay() {
        int dayOfWeek = date.getDayOfWeek();
        switch (dayOfWeek) {
            case DateTimeConstants.SUNDAY: return DaysOfWeek.Sunday;
            case DateTimeConstants.MONDAY: return DaysOfWeek.Monday;
            case DateTimeConstants.TUESDAY: return DaysOfWeek.Tuesday;
            case DateTimeConstants.WEDNESDAY: return DaysOfWeek.Wednesday;
            case DateTimeConstants.THURSDAY: return DaysOfWeek.Thursday;
            case DateTimeConstants.FRIDAY: return DaysOfWeek.Friday;
            case DateTimeConstants.SATURDAY: return DaysOfWeek.Saturday;
        }
        throw new RuntimeException(format("Cannot find day of week for %s on %s", dayOfWeek, date));
    }
}
