package com.tramchester.graph;

import java.time.LocalTime;

import static java.lang.String.format;

public class ServiceReason {

    public static InflightChangeOfService InflightChangeOfService = new InflightChangeOfService();
    public static IsValid IsValid = new IsValid();

    public static DoesNotRunOnQueryDate DoesNotRunOnQueryDate(String diag) {
        return new DoesNotRunOnQueryDate(diag);
    }

    public boolean isValid() {
        return false;
    }

    private static class IsValid extends ServiceReason
    {
        @Override
        public boolean isValid() {
            return true;
        }
        public String toString() { return "IsValid"; }
    }

    public static ServiceReason DoesNotOperateOnTime(LocalTime queryTime, String diag) {
        return new DoesNotOperateOnTime(queryTime, diag);
    }

    private static class InflightChangeOfService extends ServiceReason
    {
        public String toString() { return "InflightChangeOfService"; }
    }

    private static abstract class HasDiag extends ServiceReason {
        protected final String diag;

        public HasDiag(String diag) {
            this.diag = diag;
        }
    }

    private static class DoesNotRunOnQueryDate extends HasDiag
    {
        public DoesNotRunOnQueryDate(String diag) {
            super(diag);
        }

        public String toString() { return "DoesNotRunOnQueryDate:"+diag; }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DoesNotRunOnQueryDate)) {
                return false;
            }
            return true;
        }
    }

    private static class DoesNotOperateOnTime extends HasDiag
    {
        private LocalTime elapsedTime;

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DoesNotOperateOnTime)) {
                return false;
            }
            DoesNotOperateOnTime other = (DoesNotOperateOnTime) obj;
            return other.elapsedTime.equals(this.elapsedTime);
        }

        public DoesNotOperateOnTime(LocalTime elapsedTime, String diag) {
            super(diag);
            this.elapsedTime = elapsedTime;
        }

        public String toString() {
            return format("DoesNotOperateOnTime:%s Time:%s", diag, elapsedTime);
        }
    }

}
