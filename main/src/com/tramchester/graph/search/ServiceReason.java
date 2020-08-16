package com.tramchester.graph.search;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Service;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.states.HowIGotHere;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static java.lang.String.format;

public abstract class ServiceReason {


    public enum ReasonCode {

        ServiceDateOk, ServiceTimeOk, NumChangesOK, TimeOk, HourOk, Reachable, ReachableNoCheck, DurationOk, WalkOk, StationOpen, Continue,

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
        OnTrain,
        NotOnVehicle,
        SeenBusStationBefore,
        TooManyChanges,
        StationClosed,

        Arrived
    }

    private static final Logger logger;
    private static final boolean debugEnabled;

    private final HowIGotHere howIGotHere;
    private final ReasonCode code;

    static {
        logger = LoggerFactory.getLogger(ServiceReason.class);
        debugEnabled = logger.isDebugEnabled() && (System.getenv("CIRCLECI") == null);
        if (debugEnabled) {
            logger.warn("Debug enabled here, performance impact");
        }
    }

    public HowIGotHere getHowIGotHere() {
        return howIGotHere;
    }

    private ServiceReason(ReasonCode code) {
        this.code = code;
        howIGotHere = HowIGotHere.None();
    }

    protected ServiceReason(ReasonCode code, HowIGotHere path) {
        this.code = code;
        this.howIGotHere = path;
    }

    public abstract String textForGraph();

    public ReasonCode getReasonCode() {
        return code;
    }

    // DEFAULT
    public boolean isValid() {
        return false;
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
        return Objects.equals(howIGotHere, that.howIGotHere) &&
                code == that.code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(howIGotHere, code);
    }

    private static class Unreachable extends ServiceReason {

        private final ReasonCode code;

        protected Unreachable(ReasonCode code, HowIGotHere path) {
                super(code , path);
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
        protected IsValid(ReasonCode code, HowIGotHere path) {
            super(code, path);
        }

        protected IsValid(ReasonCode code) {
            super(code);
        }

        @Override
        public String textForGraph() {
            return getReasonCode().name();
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }

    private static class SeenBusStationBefore extends ServiceReason
    {
        protected SeenBusStationBefore(HowIGotHere path) {

            super(ReasonCode.SeenBusStationBefore, path);
        }

        @Override
        public String textForGraph() {
            return ReasonCode.SeenBusStationBefore.name();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DoesNotRunOnQueryDate;
        }
    }

    //////////////

    private static class DoesNotRunOnQueryDate extends ServiceReason
    {
        private final IdFor<Service> nodeServiceId;

        protected DoesNotRunOnQueryDate(HowIGotHere path, IdFor<Service> nodeServiceId) {
            super(ReasonCode.NotOnQueryDate, path);
            this.nodeServiceId = nodeServiceId;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DoesNotRunOnQueryDate;
        }

        @Override
        public String textForGraph() {
            return format("%s%s%s", ReasonCode.NotOnQueryDate.name(), System.lineSeparator(), nodeServiceId);
        }
    }

    //////////////

    private static class TooManyChanges extends ServiceReason {

        protected TooManyChanges(HowIGotHere path) {
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

    private static class StationClosed extends ServiceReason {

        private final Station closed;

        protected StationClosed(HowIGotHere howIGotHere, Station closed) {
            super(ReasonCode.StationClosed, howIGotHere);
            this.closed = closed;
        }

        @Override
        public String textForGraph() {
            return format("%s%s%s", ReasonCode.StationClosed.name(), System.lineSeparator(), closed.getName());
        }
    }

    //////////////

    private static class DoesNotOperateOnTime extends ServiceReason
    {
        private final TramTime elapsedTime;

        protected DoesNotOperateOnTime(ReasonCode reasonCode, TramTime elapsedTime, HowIGotHere path) {
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

    public static IsValid IsValid(ReasonCode code, HowIGotHere path) { return new IsValid( code, path);}

    public static ServiceReason IsValid(ReasonCode code) {
        return new IsValid(code);
    }

//    public static ServiceReason DoesNotRunOnQueryDate(HowIGotHere path, String nodeServiceId) {
//        return new DoesNotRunOnQueryDate(path, nodeServiceId);
//    }

    public static ServiceReason DoesNotRunOnQueryDate(HowIGotHere path, IdFor<Service> nodeServiceId) {
        return new DoesNotRunOnQueryDate(path, nodeServiceId);
    }

    public static ServiceReason StationNotReachable(HowIGotHere path) {
        return new Unreachable(ReasonCode.NotReachable, path);
    }

    public static ServiceReason DoesNotOperateOnTime(TramTime currentElapsed, HowIGotHere path) {
        return new DoesNotOperateOnTime(ReasonCode.NotAtQueryTime, currentElapsed, path);
    }

    public static ServiceReason ServiceNotRunningAtTime(TramTime currentElapsed, HowIGotHere path) {
        return new DoesNotOperateOnTime(ReasonCode.ServiceNotRunningAtTime, currentElapsed, path) ;
    }

    public static ServiceReason TooManyChanges(HowIGotHere path) {
        return new TooManyChanges(path);
    }

    public static ServiceReason TookTooLong(TramTime currentElapsed, HowIGotHere path) {
        return new DoesNotOperateOnTime(ReasonCode.TookTooLong, currentElapsed, path);
    }

    public static ServiceReason DoesNotOperateAtHour(TramTime currentElapsed, HowIGotHere path) {
        return new DoesNotOperateOnTime(ReasonCode.NotAtHour, currentElapsed, path);
    }

    public static ServiceReason AlreadyDeparted(TramTime currentElapsed, HowIGotHere path) {
        return new DoesNotOperateOnTime(ReasonCode.AlreadyDeparted, currentElapsed, path);
    }

    public static ServiceReason Cached(TramTime currentElapsed, HowIGotHere path) {
        return new ServiceReason.DoesNotOperateOnTime(ServiceReason.ReasonCode.Cached, currentElapsed, path);
    }

    public static ServiceReason Longer(HowIGotHere path) {
        return new ServiceReason.Unreachable(ReasonCode.LongerPath, path);
    }

    public static ServiceReason PathToLong(HowIGotHere path) {
        return new ServiceReason.Unreachable(ReasonCode.PathTooLong, path);
    }

    public static ServiceReason SeenBusStationBefore(HowIGotHere path) {
        return new ServiceReason.SeenBusStationBefore(path);
    }

    public static ServiceReason StationClosed(HowIGotHere howIGotHere, Station closed) {
        return new ServiceReason.StationClosed(howIGotHere, closed);
    }

}
