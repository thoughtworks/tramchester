package com.tramchester.domain;


import org.joda.time.DateTime;

public class TramServiceDate {
    private DateTime date;

    public TramServiceDate(DateTime date) {
        this.date = date;
    }

    public TramServiceDate(String date) {
        this.date = new DateTime(Integer.parseInt(date.substring(0, 4)), Integer.parseInt(date.substring(4, 6)), Integer.parseInt(date.substring(6, 8)), 0, 0, 0);
    }

    public DateTime getDate() {
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
