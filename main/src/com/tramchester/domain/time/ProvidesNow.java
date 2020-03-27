package com.tramchester.domain.time;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ProvidesNow {
    TramTime getNow();
    LocalDate getDate();
    LocalDateTime getDateTime();
}
