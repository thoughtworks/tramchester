package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;

import java.time.LocalTime;

public class ServiceReason {
    public static InflightChangeOfService InflightChangeOfService = new InflightChangeOfService();
    public static IsValid IsValid = new IsValid();
    public static DoesNotRunOnQueryDate DoesNotRunOnQueryDate = new DoesNotRunOnQueryDate();

    static class DoesNotRunOnDay extends ServiceReason
    {
        private final DaysOfWeek day;

        public DoesNotRunOnDay(DaysOfWeek day) {
            this.day = day;
        }

        public String toString() {
            return "DoesNotRunOnDay:"+day;
        }
    }
    static class InflightChangeOfService extends ServiceReason
    {
        public String toString() { return "InflightChangeOfService"; }
    }
    static class IsValid extends ServiceReason
    {
    }
    static class DoesNotRunOnQueryDate extends ServiceReason
    {
        public String toString() { return "DoesNotRunOnQueryDate"; }
    }
    static class DoesNotOperateOnTime extends ServiceReason
    {
        private LocalTime elapsedTime;

        public DoesNotOperateOnTime(LocalTime elapsedTime) {

            this.elapsedTime = elapsedTime;
        }
        public String toString() {
            return "DoesNotOperateOnTime:"+elapsedTime;
        }
    }

}
