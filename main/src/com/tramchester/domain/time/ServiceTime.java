package com.tramchester.domain.time;

import java.time.LocalTime;

public class ServiceTime {

    // TODO Still needed? Just use TramTime?

    private final TramTime tramTime;

    private ServiceTime(TramTime tramTime) {
        this.tramTime = tramTime;
    }

    public static ServiceTime of(int hours, int minutes) {
        return new ServiceTime(TramTime.of(hours, minutes));
    }

    public static ServiceTime of(TramTime time) {
        return new ServiceTime(time);
    }

    public static ServiceTime of(LocalTime time) {
        return new ServiceTime(TramTime.of(time));
    }

    public static int diffenceAsMinutes(ServiceTime departureTime, ServiceTime arrivalTime) {
        return TramTime.diffenceAsMinutes(departureTime.tramTime, arrivalTime.tramTime);
    }

    public int getHourOfDay() {
        return tramTime.getHourOfDay();
    }

    public boolean isBefore(ServiceTime other) {
        return tramTime.isBefore(other.tramTime);
    }

    public boolean isAfter(ServiceTime other) {
        return tramTime.isAfter(other.tramTime);
    }

    public String toPattern() {
        return tramTime.toPattern();
    }

    public LocalTime asLocalTime() {
        return tramTime.asLocalTime();
    }

    public ServiceTime minusMinutes(int amount) {
        return of(tramTime.minusMinutes(amount));
    }

    public ServiceTime plusMinutes(int amount) {
        return of(tramTime.plusMinutes(amount));
    }

    public boolean isNextDay() {
        return tramTime.isNextDay();
    }

    @Override
    public String toString() {
        return "ServiceTime{" +
                ", tramTime=" + tramTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceTime that = (ServiceTime) o;

        return tramTime.equals(that.tramTime);
    }

    @Override
    public int hashCode() {
        return tramTime.hashCode();
    }

    public TramTime asTramTime() {
        return tramTime;
    }
}
