package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.search.stateMachine.RegistersStates;
import com.tramchester.graph.search.stateMachine.TowardsStationState;

public class Builders {

    protected final PlatformState.Builder platform;
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

        platform = new PlatformState.Builder();
        //registersStates.addBuilder(platform);

        minute = new MinuteState.Builder(config);
        destination = new DestinationState.Builder();
        groupedStation = new GroupedStationState.Builder();
    }

    public WalkingState.Builder towardsWalk(NotStartedState from, Class<WalkingState> towards) {
        return (WalkingState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public WalkingState.Builder towardsWalk(StationState from, Class<WalkingState> towards) {
        return (WalkingState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public HourState.Builder towardsHour(ServiceState from, Class<HourState> towards) {
        return (HourState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public TramStationState.Builder towardsStation(PlatformState platformState, Class<TramStationState> tramStationStateClass) {
        return (TramStationState.Builder) registersStates.getBuilderFor(platformState.getClass(), tramStationStateClass);
    }

    public <F extends RouteStationTripState> RouteStationStateEndTrip.Builder towardsRouteStateEndTrip(MinuteState from, Class<F> towards) {
        return (RouteStationStateEndTrip.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public RouteStationStateOnTrip.Builder towardsRouteStateOnTrip(MinuteState from, Class<RouteStationStateOnTrip> towards) {
        return (RouteStationStateOnTrip.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public RouteStationStateJustBoarded.Builder towardsRouteStationJustBoarded(PlatformState from, Class<RouteStationStateJustBoarded> towards) {
        return (RouteStationStateJustBoarded.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public RouteStationStateJustBoarded.Builder towardsRouteStationJustBoarded(NoPlatformStationState from, Class<RouteStationStateJustBoarded> towards) {
        return (RouteStationStateJustBoarded.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public NoPlatformStationState.Builder towardsNeighbourFromTramStation(TramStationState from, Class<NoPlatformStationState> towards) {
        return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
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
}
