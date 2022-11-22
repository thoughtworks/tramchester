package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.graph.search.stateMachine.states.ImmuatableTraversalState;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.TraversalState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.TripRepository;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.traversal.BranchOrderingPolicies.PREORDER_BREADTH_FIRST;
import static org.neo4j.graphdb.traversal.BranchOrderingPolicies.PREORDER_DEPTH_FIRST;
import static org.neo4j.graphdb.traversal.Uniqueness.NONE;

public class TramNetworkTraverser implements PathExpander<JourneyState> {
    private static final Logger logger = LoggerFactory.getLogger(TramNetworkTraverser.class);

    private final NodeContentsRepository nodeContentsRepository;
    private final TripRepository tripRespository;
    private final TramTime actualQueryTime;
    private final Set<Long> destinationNodeIds;
    private final LocationSet destinations;
    private final TramchesterConfig config;
    private final ServiceReasons reasons;
    private final SortsPositions sortsPosition;
    private final TraversalStateFactory traversalStateFactory;
    private final RouteCalculatorSupport.PathRequest pathRequest;
    private final ReasonsToGraphViz reasonToGraphViz;
    private final ProvidesNow providesNow;

    public TramNetworkTraverser(RouteCalculatorSupport.PathRequest pathRequest,
                                SortsPositions sortsPosition, NodeContentsRepository nodeContentsRepository, TripRepository tripRespository,
                                TraversalStateFactory traversalStateFactory, LocationSet destinations, TramchesterConfig config,
                                Set<Long> destinationNodeIds, ServiceReasons reasons,
                                ReasonsToGraphViz reasonToGraphViz, ProvidesNow providesNow) {
        this.sortsPosition = sortsPosition;
        this.nodeContentsRepository = nodeContentsRepository;
        this.tripRespository = tripRespository;
        this.traversalStateFactory = traversalStateFactory;
        this.destinationNodeIds = destinationNodeIds;
        this.destinations = destinations;
        this.config = config;
        this.reasons = reasons;
        this.pathRequest = pathRequest;

        this.actualQueryTime = pathRequest.getActualQueryTime();
        this.reasonToGraphViz = reasonToGraphViz;
        this.providesNow = providesNow;
    }

    public Stream<Path> findPaths(Transaction txn, Node startNode, PreviousVisits previousSuccessfulVisit, LowestCostSeen lowestCostSeen,
                                  LowestCostsForDestRoutes lowestCostsForRoutes) {
        final boolean depthFirst = config.getDepthFirst();
        if (depthFirst) {
            logger.info("Depth first is enabled");
        } else {
            logger.info("Breadth first is enabled");
        }

        Instant begin = providesNow.getInstant();
        Duration maxInitialWait = pathRequest.getMaxInitialWait();
        final TramRouteEvaluator tramRouteEvaluator = new TramRouteEvaluator(pathRequest.getServiceHeuristics(),
                destinationNodeIds, nodeContentsRepository, reasons, previousSuccessfulVisit, lowestCostSeen, config,
                startNode.getId(), begin, providesNow, pathRequest.getRequestedModes(), maxInitialWait);

        LatLong destinationLatLon = sortsPosition.midPointFrom(destinations);

        TraversalOps traversalOps = new TraversalOps(nodeContentsRepository, tripRespository, sortsPosition, destinations,
                destinationLatLon, lowestCostsForRoutes, pathRequest.getQueryDate());

        final NotStartedState traversalState = new NotStartedState(traversalOps, traversalStateFactory);
        final InitialBranchState<JourneyState> initialJourneyState = JourneyState.initialState(actualQueryTime, traversalState);

        logger.info("Create traversal for " + actualQueryTime);

        final BranchOrderingPolicies selector = depthFirst ? PREORDER_DEPTH_FIRST : PREORDER_BREADTH_FIRST;
        TraversalDescription traversalDesc =
                new MonoDirectionalTraversalDescription().
                        // api updated, the call to expand overrides any calls to relationships
//                relationships(TRAM_GOES_TO, Direction.OUTGOING).
//                relationships(BUS_GOES_TO, Direction.OUTGOING).
//                relationships(TRAIN_GOES_TO, Direction.OUTGOING).
//                relationships(FERRY_GOES_TO, Direction.OUTGOING).
//                relationships(SUBWAY_GOES_TO, Direction.OUTGOING).
//                relationships(BOARD, Direction.OUTGOING).
//                relationships(DEPART, Direction.OUTGOING).
//                relationships(INTERCHANGE_BOARD, Direction.OUTGOING).
//                relationships(INTERCHANGE_DEPART, Direction.OUTGOING).
//                relationships(DIVERSION_DEPART, Direction.OUTGOING).
//                relationships(WALKS_TO_STATION, Direction.OUTGOING).
//                relationships(WALKS_FROM_STATION, Direction.OUTGOING).
//                relationships(ENTER_PLATFORM, Direction.OUTGOING).
//                relationships(LEAVE_PLATFORM, Direction.OUTGOING).
//                relationships(TO_SERVICE, Direction.OUTGOING).
//                relationships(TO_HOUR, Direction.OUTGOING).
//                relationships(TO_MINUTE, Direction.OUTGOING).
//                relationships(NEIGHBOUR, Direction.OUTGOING).
//                relationships(GROUPED_TO_CHILD, Direction.OUTGOING).
//                relationships(GROUPED_TO_PARENT, Direction.OUTGOING).
//                relationships(DIVERSION, Direction.OUTGOING).
                expand(this, initialJourneyState).
                evaluator(tramRouteEvaluator).
                uniqueness(NONE).
                order(selector);

        Traverser traverse = traversalDesc.traverse(startNode);
        Spliterator<Path> spliterator = traverse.spliterator();

        Stream<Path> stream = StreamSupport.stream(spliterator, false);

        //noinspection ResultOfMethodCallIgnored
        stream.onClose(() -> {
            reasons.reportReasons(txn, pathRequest, reasonToGraphViz);
            previousSuccessfulVisit.reportStats();
            traversalState.dispose();
        });

        logger.info("Return traversal stream");
        return stream.filter(path -> destinationNodeIds.contains(path.endNode().getId()));
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<JourneyState> graphState) {
        final ImmutableJourneyState currentState = graphState.getState();
        final ImmuatableTraversalState traversalState = currentState.getTraversalState();

        final Node endNode = path.endNode();
        final JourneyState journeyStateForChildren = JourneyState.fromPrevious(currentState);

        Duration cost = Duration.ZERO;
        Relationship lastRelationship = path.lastRelationship();
        if (lastRelationship !=null) {
            cost = nodeContentsRepository.getCost(lastRelationship);
            if (Durations.greaterThan(cost, Duration.ZERO)) {
                final Duration totalCost = currentState.getTotalDurationSoFar();
                Duration total = totalCost.plus(cost);
                journeyStateForChildren.updateTotalCost(total);
            }
            if (lastRelationship.isType(DIVERSION)) {
                Node startOfDiversionNode = lastRelationship.getStartNode();
                journeyStateForChildren.beginDiversion(startOfDiversionNode);
            }
        }

        final EnumSet<GraphLabel> labels = nodeContentsRepository.getLabels(endNode);

        final TraversalState traversalStateForChildren = traversalState.nextState(labels, endNode,
                journeyStateForChildren, cost, journeyStateForChildren.isOnDiversion());

        journeyStateForChildren.updateTraversalState(traversalStateForChildren);
        graphState.setState(journeyStateForChildren);

        return traversalStateForChildren.getOutbounds();
    }

    @Override
    public PathExpander<JourneyState> reverse() {
        return null;
    }


}
