package com.tramchester.domain.time;

import java.time.LocalDate;

public interface ProvidesNow {
    TramTime getNow();
    LocalDate getDate();
}
