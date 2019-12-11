package com.tramchester.graph;

import com.tramchester.domain.TramTime;
import org.neo4j.graphdb.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

public abstract class ServiceReason {

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

    private static final Logger logger = LoggerFactory.getLogger(HasDiag.class);

    protected final String pathAsString;
    private final Set<String> pathAsStrings;
    private final ReasonCode code;

    public ServiceReason(ReasonCode code, Path path, String diagnostics) {
        this.code = code;
        pathAsStrings = new HashSet<>();
        if (logger.isDebugEnabled()) {
            pathAsString = path.toString();
            pathAsStrings.addAll(PathToGraphViz.map(path, diagnostics, isValid()));
        } else {
            pathAsString="";
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

    //////////////

    private static class IsValid extends HasDiag
    {
        public IsValid(Path path, String diag) {
            super(ReasonCode.Valid, "ok:"+diag, path);
        }

        @Override
        public boolean isValid() {
            return true;
        }

    }

    private static class InflightChangeOfService extends HasDiag
    {
        public InflightChangeOfService(String diag, Path path) {
            super(ReasonCode.InflightChangeOfService, "ChangeOfSvc:"+diag, path);
        }
    }

    private static class StationNotReachable extends ServiceReason {
        public StationNotReachable(Path path) {
            super(ReasonCode.NotReachable,path,"unreachable");
        }
    }

    private static class DoesNotRunOnQueryDate extends HasDiag
    {
        public DoesNotRunOnQueryDate(String nodeServiceId, Path path) {

            super(ReasonCode.NotOnQueryDate, "NotQueryDateOrDay:"+nodeServiceId, path);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DoesNotRunOnQueryDate;
        }
    }

    private static class DoesNotOperateOnTime extends HasDiag
    {
        private LocalTime elapsedTime;

        public DoesNotOperateOnTime(ReasonCode reasonCode, LocalTime currentElapsed, String diagnostics, Path path) {
            super(reasonCode, format("%s Time:%s %s", reasonCode, currentElapsed, diagnostics), path);
            this.elapsedTime = currentElapsed;
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

    private static abstract class HasDiag extends ServiceReason {

        final String diag;

        HasDiag(ReasonCode reasonCode, String diagnostics, Path path) {
            super(reasonCode, path, diagnostics);
            this.diag = diagnostics;
        }

        @Override
        public String toString() {
            return format("diag:'%s' path:'%s'", diag, pathAsString);
        }

    }

    /// convenience methods

    public static InflightChangeOfService InflightChangeOfService(String diag, Path path) { return new InflightChangeOfService(diag, path);}

    public static IsValid IsValid(Path path, String diag) { return new IsValid(path,diag);}

    public static DoesNotRunOnQueryDate DoesNotRunOnQueryDate(String diag, Path path) {
        return new DoesNotRunOnQueryDate(diag, path);
    }

    public static ServiceReason DoesNotOperateOnTime(TramTime currentElapsed, String diagnostics, Path path) {
        return new DoesNotOperateOnTime(ReasonCode.NotAtQueryTime, currentElapsed.asLocalTime(), diagnostics, path);
    }

    public static ServiceReason StationNotReachable(Path path) {
        return new StationNotReachable(path);
    }

    public static ServiceReason ServiceNotRunningAtTime(TramTime currentElapsed, String diag, Path path) {
        return new DoesNotOperateOnTime(ReasonCode.ServiceNotRunningAtTime, currentElapsed.asLocalTime(), diag, path) ;
    }

    public static ServiceReason TookTooLong(TramTime currentElapsed, Path path) {
        return new DoesNotOperateOnTime(ReasonCode.TookTooLong, currentElapsed.asLocalTime(), "too long", path);
    }

    public static ServiceReason DoesNotOperateAtHour(TramTime currentElapsed, String diag, Path path) {
        return new DoesNotOperateOnTime(ReasonCode.NotAtHour, currentElapsed.asLocalTime(), diag, path);
    }

}
