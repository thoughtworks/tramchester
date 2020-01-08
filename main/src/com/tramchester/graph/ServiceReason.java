package com.tramchester.graph;

import com.tramchester.domain.TramTime;
import org.neo4j.graphdb.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

public abstract class ServiceReason {

    public static final IsValid isValid = new IsValid();

    public enum ReasonCode {
        Valid,
        InflightChangeOfService,
        NotOnQueryDate,
        NotAtQueryTime,
        NotReachable,
        ServiceNotRunningAtTime,
        TookTooLong,
        NotAtHour
    }

    private static final Logger logger = LoggerFactory.getLogger(ServiceReason.class);

    private final Set<String> pathAsStrings;
    private final ReasonCode code;

    public ServiceReason(ReasonCode code) {
        this.code = code;
        pathAsStrings = Collections.emptySet();
    }

    public ServiceReason(ReasonCode code, Path path) {
        this.code = code;
        if (logger.isDebugEnabled()) {
            pathAsStrings = new HashSet<>();
            pathAsStrings.addAll(PathToGraphViz.map(path, code.toString(), isValid()));
        } else {
            pathAsStrings = Collections.emptySet();
        }
    }

    public ReasonCode getReasonCode() {
        return code;
    }

    // DEFAULT
    public boolean isValid() {
        return false;
    }

    public void recordPath(Set<String> builder) {
        builder.addAll(pathAsStrings);
    }

    @Override
    public String toString() {
        return code.toString();
    }

    //////////////

    private static abstract class HasDiag extends ServiceReason {

        final String diag;
        final String pathAsString;

        HasDiag(ReasonCode reasonCode, String diagnostics, Path path) {
            super(reasonCode, path);
            pathAsString = path.toString();
            this.diag = diagnostics;
        }

        @Override
        public String toString() {
            return format("diag:'%s' path:'%s'", diag, pathAsString);
        }

    }

    //////////////

    private static class IsValid extends ServiceReason
    {
        public IsValid(Path path) {
            super(ReasonCode.Valid, path);
        }

        public IsValid() {
            super(ReasonCode.Valid);
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }

    //////////////

    private static class InflightChangeOfService extends HasDiag
    {
        public InflightChangeOfService(String diag, Path path) {
            super(ReasonCode.InflightChangeOfService, diag, path);
        }
    }

    //////////////

    private static class StationNotReachable extends ServiceReason {
        public StationNotReachable(Path path) {
            super(ReasonCode.NotReachable,path);
        }
    }

    //////////////

    private static class DoesNotRunOnQueryDateWithDiag extends HasDiag
    {
        public DoesNotRunOnQueryDateWithDiag(String nodeServiceId, Path path) {

            super(ReasonCode.NotOnQueryDate, nodeServiceId, path);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DoesNotRunOnQueryDateWithDiag;
        }
    }

    //////////////

    private static class DoesNotRunOnQueryDate extends ServiceReason
    {
        public DoesNotRunOnQueryDate() {
            super(ReasonCode.NotOnQueryDate);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DoesNotRunOnQueryDate;
        }
    }

    //////////////

    private static class DoesNotOperateOnTime extends ServiceReason
    {
        private TramTime elapsedTime;

        public DoesNotOperateOnTime(ReasonCode reasonCode, TramTime currentElapsed, Path path) {
            super(reasonCode, path);
            this.elapsedTime = currentElapsed;
        }

        @Override
        public String toString() {
            return "time:"+elapsedTime.toString()+super.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DoesNotOperateOnTime)) {
                return false;
            }
            DoesNotOperateOnTime other = (DoesNotOperateOnTime) obj;
            return other.elapsedTime.equals(this.elapsedTime);
        }
    }

    ///////////////////////////////////
    /// convenience methods

    public static InflightChangeOfService InflightChangeOfService(String diag, Path path) { return new InflightChangeOfService(diag, path);}

    public static IsValid IsValid(Path path) { return new IsValid(path);}

    public static ServiceReason DoesNotRunOnQueryDate(String diag, Path path) {
        return new DoesNotRunOnQueryDateWithDiag(diag, path);
    }

    public static ServiceReason DoesNotRunOnQueryDate() {
        return new DoesNotRunOnQueryDate();
    }

    public static ServiceReason StationNotReachable(Path path) {
        return new StationNotReachable(path);
    }

    public static ServiceReason DoesNotOperateOnTime(TramTime currentElapsed, Path path) {
        return new DoesNotOperateOnTime(ReasonCode.NotAtQueryTime, currentElapsed, path);
    }

    public static ServiceReason ServiceNotRunningAtTime(TramTime currentElapsed, Path path) {
        return new DoesNotOperateOnTime(ReasonCode.ServiceNotRunningAtTime, currentElapsed, path) ;
    }

    public static ServiceReason TookTooLong(TramTime currentElapsed, Path path) {
        return new DoesNotOperateOnTime(ReasonCode.TookTooLong, currentElapsed, path);
    }

    public static ServiceReason DoesNotOperateAtHour(TramTime currentElapsed, Path path) {
        return new DoesNotOperateOnTime(ReasonCode.NotAtHour, currentElapsed, path);
    }

}
