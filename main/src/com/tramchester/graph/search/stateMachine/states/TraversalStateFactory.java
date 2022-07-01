package com.tramchester.graph.search.stateMachine.states;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.stateMachine.RegistersStates;
import com.tramchester.graph.search.stateMachine.Towards;
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
    private final NodeContentsRepository nodeContents;
    private boolean running;

    @Inject
    public TraversalStateFactory(RegistersStates registersStates, NodeContentsRepository nodeContents, TramchesterConfig config) {
        this.registersStates = registersStates;
        this.nodeContents = nodeContents;
        this.config = config;
        running = false;
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        final boolean depthFirst = config.getDepthFirst();
        boolean interchangesOnly = config.getChangeAtInterchangeOnly();

        registersStates.addBuilder(new RouteStationStateOnTrip.Builder(interchangesOnly, nodeContents));
        registersStates.addBuilder(new RouteStationStateEndTrip.Builder(interchangesOnly));
        registersStates.addBuilder(new HourState.Builder(depthFirst, nodeContents));
        registersStates.addBuilder(new JustBoardedState.Builder(depthFirst, interchangesOnly));
        registersStates.addBuilder(new NoPlatformStationState.Builder());
        registersStates.addBuilder(new PlatformStationState.Builder());
        registersStates.addBuilder(new WalkingState.Builder());
        registersStates.addBuilder(new ServiceState.Builder(depthFirst, nodeContents));
        registersStates.addBuilder(new PlatformState.Builder());
        registersStates.addBuilder(new MinuteState.Builder(interchangesOnly, nodeContents));
        registersStates.addBuilder(new DestinationState.Builder());
        registersStates.addBuilder(new GroupedStationState.Builder());

        running = true;
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("stopping");
        running = false;
        registersStates.clear();
        logger.info("stopped");
    }

    private <S extends TraversalState, T extends Towards<S>> T getFor(Class<? extends TraversalState> from, Class<S> to) {
        if (!running) {
            // help to diagnose / pinpoint issues with timeout causing shutdown from integration tests
            throw new RuntimeException("Not running");
        }
        return registersStates.getBuilderFor(from,to);
    }

    public HourState.Builder getTowardsHour(Class<? extends TraversalState> from) {
        return getFor(from, HourState.class);
    }

    public PlatformStationState.Builder getTowardsStation(Class<? extends TraversalState> from) {
        return getFor(from, PlatformStationState.class);
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

    public JustBoardedState.Builder getTowardsJustBoarded(Class<? extends TraversalState> from) {
        return getFor(from, JustBoardedState.class);
    }

    public RouteStationStateOnTrip.Builder getTowardsRouteStationOnTrip(Class<? extends TraversalState> from) {
        return getFor(from, RouteStationStateOnTrip.class);
    }

    public RouteStationStateEndTrip.Builder getTowardsRouteStationEndTrip(Class<? extends TraversalState> from) {
        return getFor(from, RouteStationStateEndTrip.class);
    }
}
