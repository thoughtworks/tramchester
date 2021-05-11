package com.tramchester.graph.search.stateMachine.states;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.search.stateMachine.RegistersStates;
import com.tramchester.graph.search.stateMachine.TowardsState;
import com.tramchester.graph.search.stateMachine.TowardsStationState;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

@LazySingleton
public class TraversalStateFactory {
    private static final Logger logger = LoggerFactory.getLogger(TraversalStateFactory.class);

    private final RegistersStates registersStates;
    private final TramchesterConfig config;

    @Inject
    public TraversalStateFactory(RegistersStates registersStates, TramchesterConfig config) {
        this.registersStates = registersStates;
        this.config = config;
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        registersStates.addBuilder(new RouteStationStateOnTrip.Builder());
        registersStates.addBuilder(new RouteStationStateEndTrip.Builder());
        registersStates.addBuilder(new HourState.Builder());
        registersStates.addBuilder(new JustBoardedState.Builder());
        registersStates.addBuilder(new NoPlatformStationState.Builder());
        registersStates.addBuilder(new TramStationState.Builder());
        registersStates.addBuilder(new WalkingState.Builder());
        registersStates.addBuilder(new ServiceState.Builder());
        registersStates.addBuilder(new PlatformState.Builder());
        registersStates.addBuilder(new MinuteState.Builder(config));
        registersStates.addBuilder(new DestinationState.Builder());
        registersStates.addBuilder(new GroupedStationState.Builder());
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("stopping");
        registersStates.clear();
        logger.info("stopped");
    }

    public RouteStationStateEndTrip.Builder towardsRouteStateEndTrip(MinuteState from) {
        return (RouteStationStateEndTrip.Builder) registersStates.getBuilderFor(from.getClass(), RouteStationStateEndTrip.class);
    }

    public RouteStationStateOnTrip.Builder towardsRouteStateOnTrip(MinuteState from) {
        return (RouteStationStateOnTrip.Builder) registersStates.getBuilderFor(from.getClass(), RouteStationStateOnTrip.class);
    }

    public JustBoardedState.Builder towardsJustBoarded(PlatformState from) {
        return (JustBoardedState.Builder) registersStates.getBuilderFor(from.getClass(), JustBoardedState.class);
    }

    public JustBoardedState.Builder towardsJustBoarded(NoPlatformStationState from) {
        return (JustBoardedState.Builder) registersStates.getBuilderFor(from.getClass(), JustBoardedState.class);
    }

    // for multi-label
    @Deprecated
    public NoPlatformStationState.Builder towardsNoPlatformStation(RouteStationTripState from) {
        return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), NoPlatformStationState.class);
    }

    // For multi-label
    @Deprecated
    public <S extends StationState, B extends TowardsStationState<S>> B towardsStation(NotStartedState from, Class<S> towards) {
        return (B) registersStates.getBuilderFor(from.getClass(), towards);
    }

    public DestinationState.Builder towardsDest(TramStationState from) {
        return getDestBuilder(from.getClass());
    }

    public DestinationState.Builder towardsDest(NoPlatformStationState from) {
        return getDestBuilder(from.getClass());
    }

    public DestinationState.Builder towardsDest(RouteStationTripState from) {
        return getDestBuilder(from.getClass());
    }

    private DestinationState.Builder getDestBuilder(Class<? extends TraversalState> aClass) {
        return (DestinationState.Builder) registersStates.getBuilderFor(aClass, DestinationState.class);
    }

    // NEW /////////////////

    private <S extends TraversalState, T extends TowardsState<S>> T getFor(Class<? extends TraversalState> from, Class<S> to) {
        return (T) registersStates.getBuilderFor(from,to);
    }

    public HourState.Builder getTowardsHour(Class<? extends TraversalState> from) {
        return getFor(from, HourState.class);
    }

    public TramStationState.Builder getTowardsStation(Class<? extends TraversalState> from) {
        return getFor(from, TramStationState.class);
    }

    public DestinationState.Builder getTowardsDestination(Class<? extends TraversalState> from) {
        return getFor(from, DestinationState.class);
    }

    public MinuteState.Builder getTowardsMinute(Class<? extends TraversalState> from) {
        return getFor(from, MinuteState.class);
    }

    public GroupedStationState.Builder getTowardsGroup(Class<? extends TraversalState> from) {
        return getFor(from, GroupedStationState.class);
    }

    public NoPlatformStationState.Builder getTowardsNoPlatformStation(Class<? extends TraversalState> from) {
        return getFor(from, NoPlatformStationState.class);
    }

    public ServiceState.Builder getTowardsService(Class<? extends TraversalState> from) {
        return getFor(from, ServiceState.class);
    }

    public PlatformState.Builder getTowardsPlatform(Class<? extends TraversalState> from) {
        return getFor(from, PlatformState.class);
    }

    public WalkingState.Builder getTowardsWalk(Class<? extends TraversalState> from) {
        return getFor(from, WalkingState.class);
    }

}
