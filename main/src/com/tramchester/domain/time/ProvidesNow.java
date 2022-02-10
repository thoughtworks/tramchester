package com.tramchester.domain.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ProvidesNow {
    TramTime getNowHourMins();
    LocalDate getDate();
    LocalDateTime getDateTime();
    Instant getInstant();
}
