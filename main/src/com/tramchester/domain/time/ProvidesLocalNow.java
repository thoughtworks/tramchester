package com.tramchester.domain.time;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;

import java.time.*;

@LazySingleton
public class ProvidesLocalNow implements ProvidesNow {

    private final Clock clock = Clock.system(TramchesterConfig.TimeZone);

    private LocalDateTime getLocalNow() {
        return ZonedDateTime.now(TramchesterConfig.TimeZone).toLocalDateTime();
    }

    @Override
    public TramTime getNowHourMins() {
        return TramTime.ofHourMins(getLocalNow().toLocalTime());
    }

    @Override
    public LocalDate getDate() {
        return getLocalNow().toLocalDate();
    }

    @Override
    public LocalDateTime getDateTime() {
        return getLocalNow();
    }

    @Override
    public Instant getInstant() {
        return Instant.now(clock);
    }

}
