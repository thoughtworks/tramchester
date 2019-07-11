package com.tramchester.healthchecks;

import com.tramchester.domain.TramTime;

import java.time.LocalDate;

public interface ProvidesNow {
    TramTime getNow();
    LocalDate getDate();
}
