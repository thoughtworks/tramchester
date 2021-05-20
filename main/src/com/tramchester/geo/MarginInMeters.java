package com.tramchester.geo;

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

    public long get() {
        return meters;
    }

    @Override
    public String toString() {
        return "MarginInMeters{" +
                "meters=" + meters +
                '}';
    }
}
