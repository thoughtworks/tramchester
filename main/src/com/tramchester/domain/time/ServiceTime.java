package com.tramchester.domain.time;

import java.time.LocalTime;
import java.util.Optional;

public class ServiceTime {

    private static final ServiceTime[][][] serviceTimes = new ServiceTime[2][24][60];

    static {
        for(int day=0; day < 2; day++) {
            for (int hour = 0; hour < 24; hour++) {
                for (int minute = 0; minute < 60; minute++) {
                    serviceTimes[day][hour][minute] =
                            new ServiceTime(day==1? TramTime.nextDay(hour,minute) : TramTime.of(hour,minute));
                }
            }
        }
    }

    private final TramTime tramTime;

    private ServiceTime(TramTime tramTime) {
        this.tramTime = tramTime;
    }

    public static ServiceTime of(int hours, int minutes) {
        return serviceTimes[0][hours][minutes];
    }

    public static ServiceTime of(int hours, int minutes, boolean followingDay) {
        return serviceTimes[followingDay?1:0][hours][minutes];
    }

    private static ServiceTime of(TramTime time) {
        return of(time.getHourOfDay(), time.getMinuteOfHour(), time.isNextDay());
    }

    public static ServiceTime of(LocalTime time) {
        return serviceTimes[0][time.getHour()][time.getMinute()];
    }

    public static Optional<ServiceTime> parseTime(String text) {
        Optional<TramTime> maybe = TramTime.parse(text);
        if (maybe.isEmpty()) {
            return Optional.empty();
        }
        TramTime time = maybe.get();
        return Optional.of(ServiceTime.of(time));
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

    public boolean getFollowingDay() {
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
