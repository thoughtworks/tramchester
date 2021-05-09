package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.search.stateMachine.RegistersStates;
import com.tramchester.graph.search.stateMachine.TowardsStationState;

public class Builders {

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
        registersStates.addBuilder(new MinuteState.Builder(config));
        registersStates.addBuilder(new DestinationState.Builder());
        registersStates.addBuilder(new GroupedStationState.Builder());
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

    public MinuteState.Builder towardsMinute(HourState from) {
        return (MinuteState.Builder) registersStates.getBuilderFor(from.getClass(), MinuteState.class);
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

    public NoPlatformStationState.Builder towardsNoPlatformStation(RouteStationTripState from) {
        return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), NoPlatformStationState.class);
    }

    public <F extends StationState, S extends StationState, B extends TowardsStationState<S>> B towardsNeighbour(F from, Class<S> towards) {
        return (B) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public <S extends StationState, B extends TowardsStationState<S>> B towardsStation(NotStartedState from, Class<S> towards) {
        return (B) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public <S extends StationState, B extends TowardsStationState<S>> B towardsStation(WalkingState from, Class<S> towards) {
        return (B) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public <S extends StationState, B extends TowardsStationState<S>> B towardsStation(GroupedStationState from, Class<S> towards) {
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

    public DestinationState.Builder towardsDest(WalkingState from) {
        return getDestBuilder(from.getClass());
    }

    public DestinationState.Builder towardsDest(TramStationState from) {
        return getDestBuilder(from.getClass());
    }

    public DestinationState.Builder towardsDest(NoPlatformStationState from) {
        return getDestBuilder(from.getClass());
    }

    public DestinationState.Builder towardsDest(PlatformState from) {
        return getDestBuilder(from.getClass());
    }

    public DestinationState.Builder towardsDest(RouteStationTripState from) {
        return getDestBuilder(from.getClass());
    }

    public DestinationState.Builder towardsDest(GroupedStationState from) {
        return getDestBuilder(from.getClass());
    }

    private DestinationState.Builder getDestBuilder(Class<? extends TraversalState> aClass) {
        return (DestinationState.Builder) registersStates.getBuilderFor(aClass, DestinationState.class);
    }

    public GroupedStationState.Builder towardsGroup(StationState from) {
        return (GroupedStationState.Builder) registersStates.getBuilderFor(from.getClass(), GroupedStationState.class);
    }

    public GroupedStationState.Builder towardsGroup(WalkingState from) {
        return (GroupedStationState.Builder) registersStates.getBuilderFor(from.getClass(), GroupedStationState.class);
    }

    public GroupedStationState.Builder towardsGroup(NotStartedState from) {
        return (GroupedStationState.Builder) registersStates.getBuilderFor(from.getClass(), GroupedStationState.class);
    }
}
