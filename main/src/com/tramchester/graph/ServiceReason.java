package com.tramchester.graph;

import org.neo4j.graphdb.Path;

import java.time.LocalTime;

import static java.lang.String.format;

public abstract class ServiceReason {

    public static InflightChangeOfService InflightChangeOfService(String diag, Path path) { return new InflightChangeOfService(diag, path);}

    public static IsValid IsValid = new IsValid();

    public static DoesNotRunOnQueryDate DoesNotRunOnQueryDate(String diag, Path path) {
        return new DoesNotRunOnQueryDate(diag, path);
    }

    public static ServiceReason DoesNotOperateOnTime(LocalTime currentElapsed, String diagnostics, Path path) {
        return new DoesNotOperateOnTime(currentElapsed, diagnostics, path);
    }

    // DEFAULT
    public boolean isValid() {
        return false;
    }

    public abstract void recordPath(StringBuilder builder);

    private static class IsValid extends ServiceReason
    {
        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void recordPath(StringBuilder builder) {
            // noop
        }

        public String toString() { return "IsValid"; }
    }

    private static class InflightChangeOfService extends HasDiag
    {
        public InflightChangeOfService(String diag, Path path) {
            super(diag, path);
        }

        public String toString() { return "InflightChangeOfService "+diag; }
    }

    private static class DoesNotRunOnQueryDate extends HasDiag
    {

        public DoesNotRunOnQueryDate(String nodeServiceId, Path path) {
            super(nodeServiceId, path);
        }

        public String toString() {
            return "DoesNotRunOnQueryDateOrDay:" + super.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DoesNotRunOnQueryDate;
        }
    }

    private static class DoesNotOperateOnTime extends HasDiag
    {
        private LocalTime elapsedTime;

        public DoesNotOperateOnTime(LocalTime currentElapsed, String diagnostics, Path path) {
            super(diagnostics, path);
            this .elapsedTime = currentElapsed;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DoesNotOperateOnTime)) {
                return false;
            }
            DoesNotOperateOnTime other = (DoesNotOperateOnTime) obj;
            return other.elapsedTime.equals(this.elapsedTime);
        }

        public String toString() {
            return "DoesNotOperateOnTime:" + super.toString();
        }
    }

    private static abstract class HasDiag extends ServiceReason {
        protected final String diag;
        private final String pathAsString;

        public HasDiag(String diagnostics, Path path) {
            this.diag =diagnostics;
            this.pathAsString = PathToGraphViz.map(path);
        }

        @Override
        public String toString() {
            if (pathAsString!=null) {
                return format("diag:'%s' path:'%s'", diag, pathAsString);
            } else {
                return format("diag:'%s'", diag);
            }
        }

        @Override
        public void recordPath(StringBuilder builder) {
            builder.append(pathAsString);
        }
    }

}
