package com.tramchester.graph.search;

import com.tramchester.domain.time.TramTime;
import org.neo4j.graphdb.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.lang.String.format;

public abstract class ServiceReason {
    private static final Logger logger = LoggerFactory.getLogger(ServiceReason.class);

    public static final IsValid isValid = new IsValid();
    private static final boolean debugEnabled = logger.isDebugEnabled();

    public enum ReasonCode {
        Valid,
        NotOnQueryDate,
        NotAtQueryTime,
        NotReachable,
        ServiceNotRunningAtTime,
        TookTooLong,
        NotAtHour,
        AlreadyDeparted,
        Cached,
        LongerPath,
        PathTooLong,
        OnTram,
        OnBus,
        RouteAlreadySeen,
        TooManyChanges
    }

    private final Set<PathToGraphViz.RenderLater> pathToRenderAsString;
    private final ReasonCode code;

    public ServiceReason(ReasonCode code) {
        this.code = code;
        pathToRenderAsString = Collections.emptySet();
    }

    protected ServiceReason(ReasonCode code, Path path) {
        this.code = code;
        if (debugEnabled) {
            pathToRenderAsString = new HashSet<>();
            pathToRenderAsString.addAll(PathToGraphViz.map(path, this, this.isValid()));
        } else {
            pathToRenderAsString = Collections.emptySet();
        }
    }

    public abstract String textForGraph();

    public ReasonCode getReasonCode() {
        return code;
    }

    // DEFAULT
    public boolean isValid() {
        return false;
    }

    public void recordPath(Set<String> builder) {
        pathToRenderAsString.forEach(renderLater -> builder.add(renderLater.render()));
    }

    @Override
    public String toString() {
        return code.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceReason that = (ServiceReason) o;
        return Objects.equals(pathToRenderAsString, that.pathToRenderAsString) &&
                code == that.code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathToRenderAsString, code);
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

        @Override
        public String textForGraph() {
            return format("%s%s%s", getReasonCode().name(), System.lineSeparator(), diag);
        }
    }

    //////////////

    private static class Unreachable extends ServiceReason {

        private final ReasonCode code;

        public Unreachable(ReasonCode code, Path path) {
                super(code , path);
            this.code = code;
        }

        @Override
        public String textForGraph() {
            return code.name();
        }
    }

    //////////////

    private static class RouteAlreadySeen extends ServiceReason {

        private final ReasonCode code;

        protected RouteAlreadySeen(ReasonCode code, Path path) {
            super(code, path);
            this.code = code;
        }

        @Override
        public String textForGraph() {
            return code.name();
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
        public String textForGraph() {
            return ReasonCode.Valid.name();
        }

        @Override
        public boolean isValid() {
            return true;
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

        @Override
        public String textForGraph() {
            return ReasonCode.NotOnQueryDate.name();
        }
    }

    //////////////

    private static class TooManyChanges extends ServiceReason {

        public TooManyChanges(Path path) {
            super(ReasonCode.TooManyChanges, path);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TooManyChanges;
        }


        @Override
        public String textForGraph() {
            return ReasonCode.TooManyChanges.name();
        }
    }

    //////////////

    private static class DoesNotOperateOnTime extends ServiceReason
    {
        private TramTime elapsedTime;

        public DoesNotOperateOnTime(ReasonCode reasonCode, TramTime elapsedTime, Path path) {
            super(reasonCode, path);
            if (elapsedTime==null) {
                throw new RuntimeException("Must provide time");
            }
            this.elapsedTime = elapsedTime;
        }

        @Override
        public String textForGraph() {
            return format("%s%s%s", getReasonCode().name(), System.lineSeparator(), elapsedTime.toPattern());
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

    public static IsValid IsValid(Path path) { return new IsValid(path);}

    public static ServiceReason DoesNotRunOnQueryDate(String diag, Path path) {
        return new DoesNotRunOnQueryDateWithDiag(diag, path);
    }

    public static ServiceReason DoesNotRunOnQueryDate() {
        return new DoesNotRunOnQueryDate();
    }

    public static ServiceReason StationNotReachable(Path path) {
        return new Unreachable(ReasonCode.NotReachable, path);
    }

    public static ServiceReason RouteAlreadySeen(Path path) {
        return new RouteAlreadySeen(ReasonCode.RouteAlreadySeen, path);
    }

    public static ServiceReason DoesNotOperateOnTime(TramTime currentElapsed, Path path) {
        return new DoesNotOperateOnTime(ReasonCode.NotAtQueryTime, currentElapsed, path);
    }

    public static ServiceReason ServiceNotRunningAtTime(TramTime currentElapsed, Path path) {
        return new DoesNotOperateOnTime(ReasonCode.ServiceNotRunningAtTime, currentElapsed, path) ;
    }

    public static ServiceReason TooManyChanges(Path path) {
        return new TooManyChanges(path);
    }

    public static ServiceReason TookTooLong(TramTime currentElapsed, Path path) {
        return new DoesNotOperateOnTime(ReasonCode.TookTooLong, currentElapsed, path);
    }

    public static ServiceReason DoesNotOperateAtHour(TramTime currentElapsed, Path path) {
        return new DoesNotOperateOnTime(ReasonCode.NotAtHour, currentElapsed, path);
    }

    public static ServiceReason AlreadyDeparted(TramTime currentElapsed, Path path) {
        return new DoesNotOperateOnTime(ReasonCode.AlreadyDeparted, currentElapsed, path);
    }

    public static ServiceReason Cached(TramTime currentElapsed, Path path) {
        return new ServiceReason.DoesNotOperateOnTime(ServiceReason.ReasonCode.Cached, currentElapsed, path);
    }

    public static ServiceReason Longer(Path path) {
        return new ServiceReason.Unreachable(ReasonCode.LongerPath, path);
    }

    public static ServiceReason PathToLong(Path path) {
        return new ServiceReason.Unreachable(ReasonCode.PathTooLong, path);
    }

}
