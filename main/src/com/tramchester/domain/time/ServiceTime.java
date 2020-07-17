package com.tramchester.domain.time;

import java.time.LocalTime;
import java.util.Optional;

public class ServiceTime {

    private static final ServiceTime[][][] serviceTimes = new ServiceTime[2][24][60];

    static {
        for(int day=0; day < 2; day++) {
            for (int hour = 0; hour < 24; hour++) {
                for (int minute = 0; minute < 60; minute++) {
                    serviceTimes[day][hour][minute] = new ServiceTime(TramTime.of(hour,minute), day==1);
                }
            }
        }
    }

    private final boolean followingDay;
    private final TramTime tramTime;

    private ServiceTime(TramTime tramTime, boolean followingDay) {
        this.tramTime = tramTime;
        this.followingDay = followingDay;
    }

    private static ServiceTime of(TramTime time, boolean followingDay) {
        return of(time.getHourOfDay(), time.getMinuteOfHour(), followingDay);
    }

    public static ServiceTime of(int hours, int minutes) {
        return serviceTimes[0][hours][minutes];
    }

    public static ServiceTime of(int hours, int minutes, boolean followingDay) {
        return serviceTimes[followingDay?1:0][hours][minutes];
    }

    public static ServiceTime of(LocalTime time) {
        return serviceTimes[0][time.getHour()][time.getMinute()];
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
        return Optional.of(ServiceTime.of(hour, minutes, nextDay));
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
        return ServiceTime.of(tramTime.minusMinutes(amount), followingDay);
    }

    public boolean getFollowingDay() {
        return followingDay;
    }
}
