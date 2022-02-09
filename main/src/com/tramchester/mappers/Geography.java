package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import tec.units.ri.quantity.Quantities;

import javax.inject.Inject;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Time;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;
import static tec.units.ri.unit.Units.*;

@LazySingleton
public class Geography {
    private final static double KILO_PER_MILE = 1.609344D;
    private final TramchesterConfig config;
    private final Quantity<Speed> walkingSpeed;

    @Inject
    public Geography(TramchesterConfig config) {
        this.config = config;
        final double metersPerSecond = (config.getWalkingMPH() * KILO_PER_MILE * 1000) / 3600D;
        walkingSpeed = Quantities.getQuantity(metersPerSecond, METRE_PER_SECOND);
    }

    public Quantity<Time> getWalkingTime(Quantity<Length> distance) {
        return distance.divide(walkingSpeed).asType(Time.class);
    }

    @Deprecated
    public int getWalkingTimeInMinutes(Quantity<Length> distance) {
        Double minutes = Math.ceil(getWalkingTime(distance).to(MINUTE).getValue().doubleValue());
        return minutes.intValue();
    }

    public Duration getWalkingDuration(Quantity<Length> distance) {
        Double seconds = getWalkingTime(distance).to(SECOND).getValue().doubleValue();
        return Duration.of(seconds.longValue(), SECONDS);
    }
}
