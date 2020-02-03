package com.tramchester.domain.time;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.healthchecks.ProvidesNow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public class ProvidesLocalNow implements ProvidesNow {
    @Override
    public TramTime getNow() {
        return TramTime.of(getLocalNow());
    }

    private LocalDateTime getLocalNow() {
        return ZonedDateTime.now(TramchesterConfig.TimeZone).toLocalDateTime();
    }

    @Override
    public LocalDate getDate() {
        return getLocalNow().toLocalDate();
    }

}
