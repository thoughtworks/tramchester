package com.tramchester.graph.search.diagnostics;

import org.neo4j.graphdb.traversal.Evaluation;

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

    Arrived;

    private static Evaluation decideEvaluationAction(ReasonCode code) {
        return switch (code) {
            case ServiceDateOk, ServiceTimeOk, NumChangesOK, NumConnectionsOk, TimeOk, HourOk, Reachable, ReachableNoCheck,
                    DurationOk, WalkOk, StationOpen, Continue, ReachableSameRoute, TransportModeOk
                    -> Evaluation.INCLUDE_AND_CONTINUE;
            case Arrived
                    -> Evaluation.INCLUDE_AND_PRUNE;
            case HigherCost, ReturnedToStart, PathTooLong, TooManyChanges, TooManyWalkingConnections, NotReachable,
                    TookTooLong, ServiceNotRunningAtTime, NotAtHour, DoesNotOperateOnTime, NotOnQueryDate, MoreChanges,
                    AlreadyDeparted, StationClosed, TooManyNeighbourConnections, TimedOut, RouteNotOnQueryDate, HigherCostViaExchange,
                    ExchangeNotReachable, TooManyRouteChangesRequired, TooManyInterchangesRequired, AlreadySeenStation,
                    TransportModeWrong, SameTrip
                    -> Evaluation.EXCLUDE_AND_PRUNE;
            case OnTram, OnBus, OnTrain, NotOnVehicle, CachedUNKNOWN, PreviousCacheMiss, NumWalkingConnectionsOk,
                    NeighbourConnectionsOk, OnShip, OnSubway, OnWalk, CachedNotAtHour,
                    CachedDoesNotOperateOnTime, CachedTooManyRouteChangesRequired, CachedRouteNotOnQueryDate,
                    CachedNotOnQueryDate, CachedTooManyInterchangesRequired
                    -> throw new RuntimeException("Unexpected reason-code during evaluation: " + code.name());
        };
    }

    public Evaluation getEvaluationAction() {
        return decideEvaluationAction(this);
    }
}
