package com.tramchester.graph;

import org.neo4j.graphdb.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

public abstract class ServiceReason {

    private static final Logger logger = LoggerFactory.getLogger(HasDiag.class);

    protected final String pathAsString;
    private final Set<String> pathAsStrings;

    public ServiceReason(Path path, String diagnostics) {

        pathAsStrings = new HashSet<>();
        if (logger.isDebugEnabled()) {
            pathAsString = path.toString();
            pathAsStrings.addAll(PathToGraphViz.map(path, diagnostics, isValid()));
        } else {
            pathAsString="";

//            pathAsString = path.toString();
//            pathAsStrings.add(pathAsString);
        }
    }

    // DEFAULT
    public boolean isValid() {
        return false;
    }

    public void recordPath(Set<String> builder) {
        builder.addAll(pathAsStrings);
    }

    private static class IsValid extends HasDiag
    {

        public IsValid(Path path, String diag) {
            super("ok:"+diag, path);
        }

        @Override
        public boolean isValid() {
            return true;
        }

    }

    private static class InflightChangeOfService extends HasDiag
    {
        public InflightChangeOfService(String diag, Path path) {
            super("ChangeOfSvc:"+diag, path);
        }
    }

    private static class DoesNotRunOnQueryDate extends HasDiag
    {

        public DoesNotRunOnQueryDate(String nodeServiceId, Path path) {

            super("NotQueryDateOrDay:"+nodeServiceId, path);
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
            super("NotAtTime:"+diagnostics, path);
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

        protected final String diag;

        public HasDiag(String diagnostics, Path path) {
            super(path, diagnostics);
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

    public static ServiceReason DoesNotOperateOnTime(LocalTime currentElapsed, String diagnostics, Path path) {
        return new DoesNotOperateOnTime(currentElapsed, diagnostics, path);
    }

}
