package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.search.stateMachine.RegistersStates;
import com.tramchester.graph.search.stateMachine.TowardsStationState;

public class Builders {

    protected final MinuteState.Builder minute;
    protected final DestinationState.Builder destination;
    protected final GroupedStationState.Builder groupedStation;
    private final RegistersStates registersStates;

    public Builders(SortsPositions sortsPositions, LatLong destinationLatLon, TramchesterConfig config) {
        registersStates = new RegistersStates();

        registersStates.addBuilder(new RouteStationStateOnTrip.Builder());
        registersStates.addBuilder(new RouteStationStateEndTrip.Builder());
        registersStates.addBuilder(new HourState.Builder());
        registersStates.addBuilder(new RouteStationStateJustBoarded.Builder(sortsPositions, destinationLatLon));
        registersStates.addBuilder(new NoPlatformStationState.Builder());
        registersStates.addBuilder(new TramStationState.Builder());
        registersStates.addBuilder(new WalkingState.Builder());
        registersStates.addBuilder(new ServiceState.Builder());
        registersStates.addBuilder(new PlatformState.Builder());

        minute = new MinuteState.Builder(config);
        destination = new DestinationState.Builder();
        groupedStation = new GroupedStationState.Builder();
    }

    public WalkingState.Builder towardsWalk(NotStartedState from) {
        return (WalkingState.Builder) registersStates.getBuilderFor(from.getClass(), WalkingState.class);
    }

    public WalkingState.Builder towardsWalk(StationState from) {
        return (WalkingState.Builder) registersStates.getBuilderFor(from.getClass(), WalkingState.class);
    }

    public HourState.Builder towardsHour(ServiceState from) {
        return (HourState.Builder) registersStates.getBuilderFor(from.getClass(), HourState.class);
    }

    public TramStationState.Builder towardsStation(PlatformState from) {
        return (TramStationState.Builder) registersStates.getBuilderFor(from.getClass(), TramStationState.class);
    }

    public RouteStationStateEndTrip.Builder towardsRouteStateEndTrip(MinuteState from) {
        return (RouteStationStateEndTrip.Builder) registersStates.getBuilderFor(from.getClass(), RouteStationStateEndTrip.class);
    }

    public RouteStationStateOnTrip.Builder towardsRouteStateOnTrip(MinuteState from) {
        return (RouteStationStateOnTrip.Builder) registersStates.getBuilderFor(from.getClass(), RouteStationStateOnTrip.class);
    }

    public RouteStationStateJustBoarded.Builder towardsRouteStationJustBoarded(PlatformState from) {
        return (RouteStationStateJustBoarded.Builder) registersStates.getBuilderFor(from.getClass(), RouteStationStateJustBoarded.class);
    }

    public RouteStationStateJustBoarded.Builder towardsRouteStationJustBoarded(NoPlatformStationState from) {
        return (RouteStationStateJustBoarded.Builder) registersStates.getBuilderFor(from.getClass(), RouteStationStateJustBoarded.class);
    }
    
    public NoPlatformStationState.Builder towardsNoPlatformStation(RouteStationTripState from, Class<NoPlatformStationState> towards) {
        return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public <F extends StationState, S extends StationState, B extends TowardsStationState<S>> B towardsNeighbour(F from, Class<S> towards) {
        return (B) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public <S extends StationState, B extends TowardsStationState<S>> B towardsNeighbour(NotStartedState from, Class<S> towards) {
        return (B) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public <S extends StationState, B extends TowardsStationState<S>> B  towardsNeighbour(WalkingState from, Class<S> towards) {
        return (B) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public <S extends StationState, B extends TowardsStationState<S>> B towardsNeighbour(GroupedStationState from, Class<S> towards) {
        return (B) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public ServiceState.Builder towardsService(RouteStationTripState from) {
        return (ServiceState.Builder) registersStates.getBuilderFor(from.getClass(), ServiceState.class);
    }

    public ServiceState.Builder towardsService(RouteStationStateJustBoarded from) {
        return (ServiceState.Builder) registersStates.getBuilderFor(from.getClass(), ServiceState.class);
    }

    public PlatformState.Builder towardsPlatform(RouteStationTripState from) {
        return (PlatformState.Builder) registersStates.getBuilderFor(from.getClass(), PlatformState.class);
    }

    public PlatformState.Builder towardsPlatform(TramStationState from) {
        return (PlatformState.Builder) registersStates.getBuilderFor(from.getClass(), PlatformState.class);
    }
}
