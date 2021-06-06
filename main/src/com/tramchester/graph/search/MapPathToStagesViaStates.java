package com.tramchester.graph.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.ConnectingStage;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.TraversalState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.TripRepository;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.GraphPropertyKey.STOP_SEQ_NUM;
import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_CHILD;
import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_PARENT;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;

@LazySingleton
public class MapPathToStagesViaStates implements PathToStages {

    private static final Logger logger = LoggerFactory.getLogger(MapPathToStagesViaStates.class);

    private final CompositeStationRepository stationRepository;
    private final PlatformRepository platformRepository;
    private final TraversalStateFactory stateFactory;
    private final NodeContentsRepository nodeContentsRepository;
    private final TripRepository tripRepository;
    private final SortsPositions sortsPosition;
    private final ObjectMapper mapper;
    private final RouteToRouteCosts routeToRouteCosts;
    private final TramchesterConfig config;

    @Inject
    public MapPathToStagesViaStates(CompositeStationRepository stationRepository, PlatformRepository platformRepository,
                                    TraversalStateFactory stateFactory, NodeContentsRepository nodeContentsRepository,
                                    TripRepository tripRepository, SortsPositions sortsPosition,
                                    ObjectMapper mapper, RouteToRouteCosts routeToRouteCosts, TramchesterConfig config) {
        this.stationRepository = stationRepository;
        this.platformRepository = platformRepository;
        this.stateFactory = stateFactory;
        this.nodeContentsRepository = nodeContentsRepository;
        this.tripRepository = tripRepository;
        this.sortsPosition = sortsPosition;

        this.mapper = mapper;
        this.routeToRouteCosts = routeToRouteCosts;
        this.config = config;
    }

    @Override
    public List<TransportStage<?, ?>> mapDirect(Transaction txn, RouteCalculator.TimedPath timedPath, JourneyRequest journeyRequest,
                                                Set<Station> endStations) {
        Path path = timedPath.getPath();
        TramTime queryTime = timedPath.getQueryTime();
        logger.info(format("Mapping path length %s to transport stages for %s at %s with %s changes",
                path.length(), journeyRequest, queryTime, timedPath.getNumChanges()));

        LatLong destinationLatLon = sortsPosition.midPointFrom(endStations);

        TraversalOps traversalOps = new TraversalOps(nodeContentsRepository, tripRepository, sortsPosition, endStations,
                destinationLatLon, routeToRouteCosts);

        MapStatesToStages mapStatesToStages = new MapStatesToStages(stationRepository, platformRepository, tripRepository, queryTime, mapper);

        TraversalState previous = new NotStartedState(traversalOps, stateFactory);

        int lastRelationshipCost = 0;
        for (Entity entity : path) {
            if (entity instanceof Relationship) {
                Relationship relationship = (Relationship) entity;
                lastRelationshipCost = nodeContentsRepository.getCost(relationship);

                logger.debug("Seen " + relationship.getType().name() + " with cost " + lastRelationshipCost);

                if (lastRelationshipCost > 0) {
                    int total = previous.getTotalCost() + lastRelationshipCost;
                    mapStatesToStages.updateTotalCost(total);
                }
                if (relationship.hasProperty(STOP_SEQ_NUM.getText())) {
                    mapStatesToStages.passStop(relationship);
                }
            } else {
                Node node = (Node) entity;
                Set<GraphBuilder.Labels> labels = GraphBuilder.Labels.from(node.getLabels());
                TraversalState next = previous.nextState(labels, node, mapStatesToStages, lastRelationshipCost);

                logger.debug("At state " + previous.getClass().getSimpleName() + " next is " + next.getClass().getSimpleName());

                previous = next;
            }
        }
        previous.toDestination(previous, path.endNode(), 0, mapStatesToStages);

        final List<TransportStage<?, ?>> stages = mapStatesToStages.getStages();
        if (stages.isEmpty()) {
            if (path.length()==2) {
                if (path.startNode().hasRelationship(OUTGOING, GROUPED_TO_PARENT) && (path.endNode().hasRelationship(INCOMING,
                        GROUPED_TO_CHILD))) {
                    addViaCompositeStation(path, journeyRequest, stages);
                }
            } else {
                logger.warn("Did not map any stages for path length:" + path.length() + " path:" + timedPath + " request: " + journeyRequest);
            }
        }
        return stages;
    }

    private void addViaCompositeStation(Path path, JourneyRequest journeyRequest, List<TransportStage<?, ?>> stages) {
        logger.info("Add ConnectingStage Journey via single composite node");

        IdFor<Station> startId = GraphProps.getStationId(path.startNode());
        IdFor<Station> endId = GraphProps.getStationId(path.endNode());

        Station start = stationRepository.getStationById(startId);
        Station end = stationRepository.getStationById(endId);
        ConnectingStage connectingStage = new ConnectingStage(start, end, 0, journeyRequest.getTime());
        stages.add(connectingStage);
    }


}
