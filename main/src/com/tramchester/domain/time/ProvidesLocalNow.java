package com.tramchester.domain.time;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;

import java.time.*;
import java.util.Objects;

@LazySingleton
public class ProvidesLocalNow implements ProvidesNow {

    private final Clock clock = Clock.system(TramchesterConfig.TimeZoneId);

    private LocalDateTime getLocalNow() {
        return ZonedDateTime.now(TramchesterConfig.TimeZoneId).toLocalDateTime();
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

    @Override
    public String toString() {
        return "ProvidesLocalNow{" +
                "clock=" + clock +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProvidesLocalNow that = (ProvidesLocalNow) o;
        return clock.equals(that.clock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clock);
    }
}
