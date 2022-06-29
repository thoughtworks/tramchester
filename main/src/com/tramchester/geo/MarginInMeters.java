package com.tramchester.geo;

import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;

public class MarginInMeters {
    private final long meters;

    public static MarginInMeters of(long meters) {
        return new MarginInMeters(meters);
    }

    private MarginInMeters(long meters) {
        this.meters = meters;
    }

    public static MarginInMeters of(Double kilometers) {
        double meters = kilometers * 1000D;
        return new MarginInMeters(Math.round(meters));
    }

    public static MarginInMeters invalid() {
        return new MarginInMeters(Long.MIN_VALUE);
    }

    /***
     * Change this to use Quantity
     * @return margin in meters
     */
    @Deprecated
    public long get() {
        return meters;
    }

    @Override
    public String toString() {
        return "MarginInMeters{" +
                "meters=" + meters +
                '}';
    }

    public boolean within(Quantity<Length> amount) {
        Quantity<Length> amountInMeters = amount.to(Units.METRE);
        Number value = amountInMeters.getValue();

        return value.longValue() <= meters;
    }
}
