package com.tramchester.domain;


import org.joda.time.LocalDate;

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
                '}';
    }
}
