package com.tramchester.domain.time;

import com.tramchester.config.TramchesterConfig;

import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Singleton
public class ProvidesLocalNow implements ProvidesNow {

    private LocalDateTime getLocalNow() {
        return ZonedDateTime.now(TramchesterConfig.TimeZone).toLocalDateTime();
    }

    @Override
    public TramTime getNow() {
        return TramTime.of(getLocalNow());
    }

    @Override
    public LocalDate getDate() {
        return getLocalNow().toLocalDate();
    }

    @Override
    public LocalDateTime getDateTime() {
        return getLocalNow();
    }

}
