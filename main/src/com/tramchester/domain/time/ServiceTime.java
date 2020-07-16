package com.tramchester.domain.time;

import java.time.LocalTime;
import java.util.Optional;

public class ServiceTime {

    private final boolean followingDay;
    private final TramTime tramTime;

    private ServiceTime(TramTime tramTime, boolean followingDay) {
        this.tramTime = tramTime;
        this.followingDay = followingDay;
    }

    private ServiceTime(int hour, int minute, boolean followingDay) {
        this(TramTime.of(hour, minute), followingDay);
    }

    private ServiceTime(int hour, int minute) {
        this(TramTime.of(hour, minute), false);
    }

    public static ServiceTime of(int hours, int minutes) {
        return new ServiceTime(hours, minutes);
    }

    public static ServiceTime of(int hours, int minutes, boolean followingDay) {
        return new ServiceTime(hours, minutes, followingDay);
    }

    public static ServiceTime of(LocalTime time) {
        return new ServiceTime(time.getHour(),time.getMinute());
    }

    public static Optional<ServiceTime> parseTime(String text) {
        String[] split = text.split(":",3);

        boolean nextDay = false;
        int hour = Integer.parseInt(split[0]);
        // gtfs standard represents service next day by time > 24:00:00
        if (hour>=24) {
            hour = hour - 24;
            nextDay = true;
        }
        if (hour>23) {
            // spanning 2 days, cannot handle yet
            return Optional.empty();
        }
        int minutes = Integer.parseInt(split[1]);
        if (minutes > 59) {
            return Optional.empty();
        }
        return Optional.of(new ServiceTime(hour, minutes, nextDay));
    }

    // TODO Use following day flag
    public static int diffenceAsMinutes(ServiceTime departureTime, ServiceTime arrivalTime) {
        return TramTime.diffenceAsMinutes(departureTime.tramTime, arrivalTime.tramTime);
    }

    public static boolean isBetween(TramTime tramTime, ServiceTime start, ServiceTime end) {
        return tramTime.between(start.tramTime, end.tramTime);
    }

    @Override
    public String toString() {
        return "ServiceTime{" +
                "followingDay=" + followingDay +
                ", tramTime=" + tramTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceTime that = (ServiceTime) o;

        if (followingDay != that.followingDay) return false;
        return tramTime == that.tramTime;
    }

    @Override
    public int hashCode() {
        int result = (followingDay ? 1 : 0);
        result = 31 * result + tramTime.hashCode();
        return result;
    }

    public String tramDataFormat() {
        return tramTime.tramDataFormat();
    }

    public int getHourOfDay() {
        return tramTime.getHourOfDay();
    }

    // TODO Use following day flag
    public boolean isBefore(ServiceTime other) {
        return tramTime.isBefore(other.tramTime);
    }

    // TODO Use following day flag
    public boolean isAfter(ServiceTime other) {
        return tramTime.isAfter(other.tramTime);
    }

    // TODO Use following day flag
    public String toPattern() {
        return tramTime.toPattern();
    }

    public LocalTime asLocalTime() {
        return tramTime.asLocalTime();
    }

    public ServiceTime minusMinutes(int amount) {
        return new ServiceTime(tramTime.minusMinutes(amount), followingDay);
    }

    public boolean getFollowingDay() {
        return followingDay;
    }
}
