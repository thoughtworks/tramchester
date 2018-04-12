package com.tramchester.acceptance.infra;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

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
