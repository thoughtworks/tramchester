package com.tramchester.acceptance.infra;


import java.time.LocalDate;
import java.time.LocalTime;

public class TramJourney {
    public final String fromStop;
    public final String toStop;
    public final LocalDate date;
    public final LocalTime time;

    public TramJourney(String fromStop, String toStop, LocalDate date, LocalTime time) {

        this.fromStop = fromStop;
        this.toStop = toStop;
        this.date = date;
        this.time = time;
    }
}
