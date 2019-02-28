package com.tramchester.graph;

import org.neo4j.graphdb.Node;

import java.time.LocalTime;

import static java.lang.String.format;

public class ServiceReason {

    public static InflightChangeOfService InflightChangeOfService(String diag) { return new InflightChangeOfService(diag);}

    public static IsValid IsValid = new IsValid();

    public static DoesNotRunOnQueryDate DoesNotRunOnQueryDate(String diag) {
        return new DoesNotRunOnQueryDate(diag);
    }

    public static ServiceReason DoesNotRunOnQueryDate(Node node, String nodeServiceId) {
        return new DoesNotRunOnQueryDate(node, nodeServiceId);
    }

    public static ServiceReason DoesNotOperateOnTime(Node node, LocalTime currentElapsed, String diagnostics) {
        return new DoesNotOperateOnTime(node, currentElapsed, diagnostics);
    }

    public static ServiceReason DoesNotOperateOnTime(LocalTime queryTime, String diag) {
        return new DoesNotOperateOnTime(queryTime, diag);
    }

    // DEFAULT
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

    private static class InflightChangeOfService extends HasDiag
    {
        public InflightChangeOfService(String diag) {
            super(diag);
        }

        public String toString() { return "InflightChangeOfService "+diag; }
    }

    private static class DoesNotRunOnQueryDate extends HasDiag
    {
        public DoesNotRunOnQueryDate(String diag) {
            super(diag);
        }

        public DoesNotRunOnQueryDate(Node node, String nodeServiceId) {
            super(node, nodeServiceId);
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

        public DoesNotOperateOnTime(Node node, LocalTime currentElapsed, String diagnostics) {
            super(node, diagnostics);
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

        public DoesNotOperateOnTime(LocalTime elapsedTime, String diag) {
            super(diag);
            this.elapsedTime = elapsedTime;
        }

        public String toString() {
            return "DoesNotOperateOnTime:" + super.toString();
        }
    }

    private static abstract class HasDiag extends ServiceReason {
        protected final String diag;
        private final Node node;

        public HasDiag(String diag) {
            this(null,diag);
        }

        public HasDiag(Node node, String diagnostics) {
            this.diag =diagnostics;
            this.node = node;
        }

        @Override
        public String toString() {
            if (node!=null) {
                return format("diag:'%s' node:'%s'", diag, node.getProperty(GraphStaticKeys.ID));
            } else {
                return format("diag:'%s'", diag);
            }
        }
    }

}
