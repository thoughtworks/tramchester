package com.tramchester.graph.search.diagnostics;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;

import static java.lang.String.format;

public abstract class ServiceReason {

    private static class Unreachable extends HeuristicsReason {

        protected Unreachable(ReasonCode code, HowIGotHere path) {
                super(code , path);
        }

        @Override
        public String textForGraph() {
            return code.name();
        }
    }

    //////////////

    private static class IsValid extends HeuristicsReason
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

    private static class Continue extends HeuristicsReason {

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

    private static class ReturnedToStart extends HeuristicsReason {
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

    private static class SameTrip extends HeuristicsReason {
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

    private static class AlreadySeenStation extends HeuristicsReason {

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

    private static class RouteNotAvailableOnQueryDate extends HeuristicsReason {
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

    private static class DoesNotRunOnQueryDate extends HeuristicsReason
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

    private static class TooManyChanges extends HeuristicsReason {

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

    private static class TimedOut extends HeuristicsReason {

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

    private static class TooManyWalkingConnections extends HeuristicsReason {

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

    private static class TooManyNeighbourConnections extends HeuristicsReason {

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

    private static class StationClosed extends HeuristicsReason {

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

    private static class TransportModeWrong extends HeuristicsReason {

        protected TransportModeWrong(HowIGotHere howIGotHere) {
            super(ReasonCode.TransportModeWrong, howIGotHere);
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

    private static class DoesNotOperateOnTime extends HeuristicsReason
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

    public static HeuristicsReason Continue(HowIGotHere path) {
        return new Continue(path);
    }

    public static HeuristicsReason DoesNotRunOnQueryDate(HowIGotHere path, IdFor<Service> nodeServiceId) {
        return new DoesNotRunOnQueryDate(path, nodeServiceId);
    }

    public static HeuristicsReason ServiceNotRunningAtTime(HowIGotHere path, IdFor<Service> serviceId, TramTime time) {
        return new ServiceDoesNotOperateOnTime(ReasonCode.ServiceNotRunningAtTime, time, path, serviceId);
    }

    public static HeuristicsReason StationNotReachable(HowIGotHere path, ReasonCode code) {
        return new Unreachable(code, path);
    }

    public static HeuristicsReason InterchangeNotReachable(HowIGotHere path) {
        return new Unreachable(ReasonCode.ExchangeNotReachable, path);
    }

    public static HeuristicsReason DoesNotOperateOnTime(TramTime currentElapsed, HowIGotHere path) {
        return new DoesNotOperateOnTime(ReasonCode.DoesNotOperateOnTime, currentElapsed, path);
    }

    public static HeuristicsReason TooManyChanges(HowIGotHere path, int number) {
        return new TooManyChanges(path, number);
    }

    public static HeuristicsReason TooManyWalkingConnections(HowIGotHere path) {
        return new TooManyWalkingConnections(path);
    }

    public static HeuristicsReason TooManyNeighbourConnections(HowIGotHere path) {
        return new TooManyNeighbourConnections(path);
    }

    public static HeuristicsReason TookTooLong(TramTime currentElapsed, HowIGotHere path) {
        return new DoesNotOperateOnTime(ReasonCode.TookTooLong, currentElapsed, path);
    }

    public static HeuristicsReason DoesNotOperateAtHour(TramTime currentElapsed, HowIGotHere path) {
        return new DoesNotOperateOnTime(ReasonCode.NotAtHour, currentElapsed, path);
    }

    public static HeuristicsReason AlreadyDeparted(TramTime currentElapsed, HowIGotHere path) {
        return new DoesNotOperateOnTime(ReasonCode.AlreadyDeparted, currentElapsed, path);
    }

    public static HeuristicsReason Cached(ReasonCode code, TramTime currentElapsed, HowIGotHere path) {

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

    public static HeuristicsReason HigherCost(HowIGotHere howIGotHere) {
        return new ServiceReason.Unreachable(ReasonCode.HigherCost, howIGotHere);
    }

    public static HeuristicsReason LongerViaInterchange(HowIGotHere howIGotHere) {
        return new ServiceReason.Unreachable(ReasonCode.HigherCostViaExchange, howIGotHere);
    }

    public static HeuristicsReason PathToLong(HowIGotHere path) {
        return new ServiceReason.Unreachable(ReasonCode.PathTooLong, path);
    }

    public static HeuristicsReason ReturnedToStart(HowIGotHere path) {
        return new ReturnedToStart(path);
    }

    public static HeuristicsReason StationClosed(HowIGotHere howIGotHere, Station closed) {
        return new StationClosed(howIGotHere, closed);
    }

    public static HeuristicsReason TimedOut(HowIGotHere howIGotHere) {
        return new TimedOut(howIGotHere);
    }


    public static HeuristicsReason TransportModeWrong(HowIGotHere howIGotHere) {
        return new ServiceReason.TransportModeWrong(howIGotHere);
    }

    public static HeuristicsReason RouteNotToday(HowIGotHere howIGotHere, IdFor<Route> id) {
        return new ServiceReason.RouteNotAvailableOnQueryDate(howIGotHere, id);
    }

    public static HeuristicsReason CacheMiss(HowIGotHere howIGotHere) {
        return new IsValid(ReasonCode.PreviousCacheMiss, howIGotHere);
    }

    public static HeuristicsReason AlreadySeenStation(IdFor<Station> stationId, HowIGotHere howIGotHere) {
        return new AlreadySeenStation(stationId, howIGotHere);
    }

    public static HeuristicsReason SameTrip(IdFor<Trip> tripId, HowIGotHere howIGotHere) {
        return new SameTrip(tripId, howIGotHere);
    }

}
