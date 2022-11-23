package com.tramchester.graph.search;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.stateMachine.HowIGotHere;

import java.util.Objects;

import static java.lang.String.format;

public abstract class ServiceReason {


    public enum ReasonCode {

        ServiceDateOk, ServiceTimeOk, NumChangesOK, TimeOk, HourOk, Reachable, ReachableNoCheck, DurationOk,
        WalkOk, StationOpen, Continue, NumConnectionsOk, NumWalkingConnectionsOk, NeighbourConnectionsOk,
        ReachableSameRoute, TransportModeOk,

        NotOnQueryDate,
        RouteNotOnQueryDate,
        DoesNotOperateOnTime,
        NotReachable,
        ExchangeNotReachable,
        ServiceNotRunningAtTime,
        TookTooLong,
        NotAtHour,
        AlreadyDeparted,
        HigherCost,
        HigherCostViaExchange,
        PathTooLong,
        AlreadySeenStation,
        TransportModeWrong,
        SameTrip,

        ReturnedToStart,
        TooManyChanges,
        MoreChanges,
        TooManyWalkingConnections,
        TooManyNeighbourConnections,
        StationClosed,
        TimedOut,
        TooManyRouteChangesRequired,
        TooManyInterchangesRequired,

        CachedUNKNOWN,
        CachedNotAtHour,
        CachedDoesNotOperateOnTime,
        CachedTooManyRouteChangesRequired,
        CachedRouteNotOnQueryDate,
        CachedNotOnQueryDate,
        CachedTooManyInterchangesRequired,
        PreviousCacheMiss,

        // stats for overall journey
        OnTram,
        OnBus,
        OnTrain,
        OnShip,
        OnSubway,
        OnWalk,
        NotOnVehicle,

        Arrived
    }

    private final HowIGotHere howIGotHere;
    private final ReasonCode code;

    public HowIGotHere getHowIGotHere() {
        return howIGotHere;
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

        @Override
        public String textForGraph() {
            return getReasonCode().name();
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }

    private static class Continue extends ServiceReason {

        public Continue(HowIGotHere path) {
            super(ReasonCode.Continue, path);
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public String textForGraph() {
            return "continue";
        }
    }

    //////////////

    private static class ReturnedToStart extends ServiceReason {
        protected ReturnedToStart(HowIGotHere path) {
            super(ReasonCode.ReturnedToStart, path);
        }

        @Override
        public String textForGraph() {
            return ReasonCode.ReturnedToStart.name();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DoesNotRunOnQueryDate;
        }
    }

    ///////

    private static class SameTrip extends ServiceReason {
        private final IdFor<Trip> tripId;

        private SameTrip(IdFor<Trip> tripId, HowIGotHere path) {
            super(ReasonCode.SameTrip, path);
            this.tripId = tripId;
        }

        @Override
        public String textForGraph() {
            return "SameTrip:"+tripId;
        }
    }


    ///////

    private static class AlreadySeenStation extends ServiceReason {

        private final IdFor<Station> stationId;

        protected AlreadySeenStation(IdFor<Station> stationId, HowIGotHere path) {
            super(ReasonCode.AlreadySeenStation, path);
            this.stationId = stationId;
        }

        @Override
        public String textForGraph() {
            return "AlreadySeenStation:"+stationId.getGraphId();
        }
    }


    ///////

    private static class RouteNotAvailableOnQueryDate extends ServiceReason {
        private final IdFor<Route> routeId;

        protected RouteNotAvailableOnQueryDate(HowIGotHere path, IdFor<Route> routeId) {
            super(ReasonCode.RouteNotOnQueryDate, path);
            this.routeId = routeId;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DoesNotRunOnQueryDate;
        }

        @Override
        public String textForGraph() {
            return format("%s%s%s", ReasonCode.RouteNotOnQueryDate.name(), System.lineSeparator(), routeId);
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

        private final int number;

        protected TooManyChanges(HowIGotHere path, int number) {
            super(ReasonCode.TooManyChanges, path);
            this.number = number;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TooManyChanges;
        }


        @Override
        public String textForGraph() {
            return ReasonCode.TooManyChanges.name() + " " + number;
        }
    }

    //////////////

    private static class TimedOut extends ServiceReason {

        protected TimedOut(HowIGotHere path) {
            super(ReasonCode.TimedOut, path);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TooManyChanges;
        }

        @Override
        public String textForGraph() {
            return ReasonCode.TimedOut.name();
        }
    }

    //////////////

    private static class TooManyWalkingConnections extends ServiceReason {

        protected TooManyWalkingConnections(HowIGotHere path) {
            super(ReasonCode.TooManyWalkingConnections, path);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TooManyChanges;
        }


        @Override
        public String textForGraph() {
            return ReasonCode.TooManyWalkingConnections.name();
        }
    }

    //////////////

    private static class TooManyNeighbourConnections extends ServiceReason {

        protected TooManyNeighbourConnections(HowIGotHere path) {
            super(ReasonCode.TooManyNeighbourConnections, path);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TooManyChanges;
        }


        @Override
        public String textForGraph() {
            return ReasonCode.TooManyNeighbourConnections.name();
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

    private static class TransportModeWrong extends ServiceReason {
        //private final IdFor<RouteStation> routeStation;

        protected TransportModeWrong(HowIGotHere howIGotHere) { //, IdFor<RouteStation> routeStation) {
            super(ReasonCode.TransportModeWrong, howIGotHere);
            //this.routeStation = routeStation;
        }

        @Override
        public String textForGraph() {
            return format("%s%s", ReasonCode.TransportModeWrong.name(), System.lineSeparator());
        }
    }

    //////////////

    private static class ServiceDoesNotOperateOnTime extends DoesNotOperateOnTime {

        private final IdFor<Service> serviceId;

        protected ServiceDoesNotOperateOnTime(ReasonCode reasonCode, TramTime elapsedTime, HowIGotHere path, IdFor<Service> serviceId) {
            super(reasonCode, elapsedTime, path);
            this.serviceId = serviceId;
        }

        @Override
        public String textForGraph() {
            return format("ServiceDoesNotOperateOnTime:%s%s%s", serviceId, System.lineSeparator(), elapsedTime);
        }
    }

    //////////////

    private static class DoesNotOperateOnTime extends ServiceReason
    {
        protected final TramTime elapsedTime;

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
            return super.toString() + " time:"+elapsedTime.toString();
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

    public static ServiceReason Continue(HowIGotHere path) {
        return new Continue(path);
    }

    public static ServiceReason DoesNotRunOnQueryDate(HowIGotHere path, IdFor<Service> nodeServiceId) {
        return new DoesNotRunOnQueryDate(path, nodeServiceId);
    }

    public static ServiceReason ServiceNotRunningAtTime(HowIGotHere path, IdFor<Service> serviceId, TramTime time) {
        return new ServiceDoesNotOperateOnTime(ReasonCode.ServiceNotRunningAtTime, time, path, serviceId);
    }

    public static ServiceReason StationNotReachable(HowIGotHere path, ReasonCode code) {
        return new Unreachable(code, path);
    }

    public static ServiceReason InterchangeNotReachable(HowIGotHere path) {
        return new Unreachable(ReasonCode.ExchangeNotReachable, path);
    }

    public static ServiceReason DoesNotOperateOnTime(TramTime currentElapsed, HowIGotHere path) {
        return new DoesNotOperateOnTime(ReasonCode.DoesNotOperateOnTime, currentElapsed, path);
    }

    public static ServiceReason TooManyChanges(HowIGotHere path, int number) {
        return new TooManyChanges(path, number);
    }

    public static ServiceReason TooManyWalkingConnections(HowIGotHere path) {
        return new TooManyWalkingConnections(path);
    }

    public static ServiceReason TooManyNeighbourConnections(HowIGotHere path) {
        return new TooManyNeighbourConnections(path);
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

    public static ServiceReason Cached(ServiceReason.ReasonCode code, TramTime currentElapsed, HowIGotHere path) {

        return switch (code) {
            case NotAtHour -> new DoesNotOperateOnTime(ReasonCode.CachedNotAtHour, currentElapsed, path);
            case DoesNotOperateOnTime -> new DoesNotOperateOnTime(ReasonCode.CachedDoesNotOperateOnTime, currentElapsed, path);
            case TooManyRouteChangesRequired -> new DoesNotOperateOnTime(ReasonCode.CachedTooManyRouteChangesRequired, currentElapsed, path);
            case RouteNotOnQueryDate -> new DoesNotOperateOnTime(ReasonCode.CachedRouteNotOnQueryDate, currentElapsed, path);
            case NotOnQueryDate -> new DoesNotOperateOnTime(ReasonCode.CachedNotOnQueryDate, currentElapsed, path);
            case TooManyInterchangesRequired -> new DoesNotOperateOnTime(ReasonCode.CachedTooManyInterchangesRequired, currentElapsed, path);
            default -> new DoesNotOperateOnTime(ReasonCode.CachedUNKNOWN, currentElapsed, path);
        };
    }

    public static ServiceReason HigherCost(HowIGotHere howIGotHere) {
        return new ServiceReason.Unreachable(ReasonCode.HigherCost, howIGotHere);
    }

    public static ServiceReason LongerViaInterchange(HowIGotHere howIGotHere) {
        return new ServiceReason.Unreachable(ReasonCode.HigherCostViaExchange, howIGotHere);
    }

    public static ServiceReason PathToLong(HowIGotHere path) {
        return new ServiceReason.Unreachable(ReasonCode.PathTooLong, path);
    }

    public static ServiceReason ReturnedToStart(HowIGotHere path) {
        return new ReturnedToStart(path);
    }

    public static ServiceReason StationClosed(HowIGotHere howIGotHere, Station closed) {
        return new StationClosed(howIGotHere, closed);
    }

    public static ServiceReason TimedOut(HowIGotHere howIGotHere) {
        return new TimedOut(howIGotHere);
    }


    public static ServiceReason TransportModeWrong(HowIGotHere howIGotHere) {
        return new ServiceReason.TransportModeWrong(howIGotHere);
    }

    public static ServiceReason RouteNotToday(HowIGotHere howIGotHere, IdFor<Route> id) {
        return new ServiceReason.RouteNotAvailableOnQueryDate(howIGotHere, id);
    }

    public static ServiceReason CacheMiss(HowIGotHere howIGotHere) {
        return new IsValid(ReasonCode.PreviousCacheMiss, howIGotHere);
    }

    public static ServiceReason AlreadySeenStation(IdFor<Station> stationId, HowIGotHere howIGotHere) {
        return new AlreadySeenStation(stationId, howIGotHere);
    }

    public static ServiceReason SameTrip(IdFor<Trip> tripId, HowIGotHere howIGotHere) {
        return new SameTrip(tripId, howIGotHere);
    }

}
