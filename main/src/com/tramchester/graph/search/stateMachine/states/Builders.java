package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.search.stateMachine.RegistersStates;

public class Builders {

    protected final TramStationState.Builder tramStation;
    protected final ServiceState.Builder service;
    protected final PlatformState.Builder platform;
    protected final WalkingState.Builder walking;
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

        service = new ServiceState.Builder();
        platform = new PlatformState.Builder();
        walking = new WalkingState.Builder();
        minute = new MinuteState.Builder(config);
        tramStation = new TramStationState.Builder();
        destination = new DestinationState.Builder();
        groupedStation = new GroupedStationState.Builder();
    }


    public RouteStationStateEndTrip.Builder towardsRouteStateEndTrip(MinuteState from, Class<RouteStationStateEndTrip> towards) {
        return (RouteStationStateEndTrip.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public RouteStationStateOnTrip.Builder towardsRouteStateOnTrip(MinuteState from, Class<RouteStationStateOnTrip> towards) {
        return (RouteStationStateOnTrip.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public HourState.Builder towardsHour(ServiceState from, Class<HourState> towards) {
        return (HourState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public RouteStationStateJustBoarded.Builder towardsRouteStationJustBoarded(PlatformState from, Class<RouteStationStateJustBoarded> towards) {
        return (RouteStationStateJustBoarded.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public RouteStationStateJustBoarded.Builder towardsRouteStationJustBoarded(NoPlatformStationState from, Class<RouteStationStateJustBoarded> towards) {
        return (RouteStationStateJustBoarded.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public NoPlatformStationState.Builder towardsNeighbour(NoPlatformStationState from, Class<NoPlatformStationState> towards) {
        return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public NoPlatformStationState.Builder towardsNeighbour(GroupedStationState from, Class<NoPlatformStationState> towards) {
        return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public NoPlatformStationState.Builder towardsNeighbour(WalkingState from, Class<NoPlatformStationState> towards) {
        return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public NoPlatformStationState.Builder towardsNeighbour(NotStartedState from, Class<NoPlatformStationState> towards) {
        return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public NoPlatformStationState.Builder towardsNeighbourFromTramStation(TramStationState from, Class<NoPlatformStationState> towards) {
        return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public NoPlatformStationState.Builder towardsNoPlatformStation(RouteStationStateEndTrip from, Class<NoPlatformStationState> towards) {
        return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public NoPlatformStationState.Builder towardsNoPlatformStation(RouteStationStateOnTrip from, Class<NoPlatformStationState> towards) {
        return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
    }
}
