package com.tramchester.graph.search.diagnostics;

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
