package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.ConnectingStage;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.TraversalState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;

import static com.tramchester.graph.GraphPropertyKey.STOP_SEQ_NUM;
import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_CHILD;
import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_PARENT;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;

@LazySingleton
public class MapPathToStagesViaStates implements PathToStages {

    private static final Logger logger = LoggerFactory.getLogger(MapPathToStagesViaStates.class);

    private final StationRepository stationRepository;
    private final PlatformRepository platformRepository;
    private final TraversalStateFactory stateFactory;
    private final StationGroupsRepository stationGroupsRepository;
    private final NodeContentsRepository nodeContentsRepository;
    private final TripRepository tripRepository;
    private final SortsPositions sortsPosition;

    @Inject
    public MapPathToStagesViaStates(StationRepository stationRepository, PlatformRepository platformRepository,
                                    TraversalStateFactory stateFactory, StationGroupsRepository stationGroupsRepository,
                                    NodeContentsRepository nodeContentsRepository,
                                    TripRepository tripRepository, SortsPositions sortsPosition) {
        this.stationRepository = stationRepository;
        this.platformRepository = platformRepository;
        this.stateFactory = stateFactory;
        this.stationGroupsRepository = stationGroupsRepository;
        this.nodeContentsRepository = nodeContentsRepository;
        this.tripRepository = tripRepository;
        this.sortsPosition = sortsPosition;

    }

    @Override
    public List<TransportStage<?, ?>> mapDirect(RouteCalculator.TimedPath timedPath, JourneyRequest journeyRequest,
                                                LowestCostsForDestRoutes lowestCostForRoutes, LocationSet endStations) {
        final Path path = timedPath.getPath();
        final TramTime queryTime = timedPath.getQueryTime();
        logger.info(format("Mapping path length %s to transport stages for %s at %s with %s changes",
                path.length(), journeyRequest, queryTime, timedPath.getNumChanges()));

        final LatLong destinationLatLon = sortsPosition.midPointFrom(endStations);

        final TraversalOps traversalOps = new TraversalOps(nodeContentsRepository, tripRepository, sortsPosition, endStations,
                destinationLatLon, lowestCostForRoutes, journeyRequest.getDate());

        final MapStatesToStages mapStatesToStages = new MapStatesToStages(stationRepository, platformRepository, tripRepository, queryTime);

        TraversalState previous = new NotStartedState(traversalOps, stateFactory, journeyRequest.getRequestedModes());

        Duration lastRelationshipCost = Duration.ZERO;
        for (Entity entity : path) {
            if (entity instanceof Relationship) {
                Relationship relationship = (Relationship) entity;
                lastRelationshipCost = nodeContentsRepository.getCost(relationship);

                logger.debug("Seen " + relationship.getType().name() + " with cost " + lastRelationshipCost);

                if (Durations.greaterThan(lastRelationshipCost, Duration.ZERO)) {
                    Duration total = previous.getTotalDuration().plus(lastRelationshipCost);
                    mapStatesToStages.updateTotalCost(total);
                }
                if (relationship.hasProperty(STOP_SEQ_NUM.getText())) {
                    mapStatesToStages.passStop(relationship);
                }
            } else {
                final Node node = (Node) entity;
                final EnumSet<GraphLabel> labels = nodeContentsRepository.getLabels(node);
                final boolean alreadyOnDiversion = false;
                final TraversalState next = previous.nextState(labels, node, mapStatesToStages, lastRelationshipCost, alreadyOnDiversion);

                logger.debug("At state " + previous.getClass().getSimpleName() + " next is " + next.getClass().getSimpleName());

                previous = next;
            }
        }
        previous.toDestination(previous, path.endNode(), Duration.ZERO, mapStatesToStages);

        final List<TransportStage<?, ?>> stages = mapStatesToStages.getStages();
        if (stages.isEmpty()) {
            if (path.length()==2) {
                if (path.startNode().hasRelationship(OUTGOING, GROUPED_TO_PARENT) &&
                        (path.endNode().hasRelationship(INCOMING, GROUPED_TO_CHILD))) {
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

        IdFor<NaptanArea> startId = GraphProps.getAreaIdFromGrouped(path.startNode());
        IdFor<NaptanArea> endId = GraphProps.getAreaIdFromGrouped(path.endNode());
        StationGroup start = stationGroupsRepository.getStationGroup(startId);
        StationGroup end = stationGroupsRepository.getStationGroup(endId);

        ConnectingStage<StationGroup, StationGroup> connectingStage =
                new ConnectingStage<>(start, end, Duration.ZERO, journeyRequest.getOriginalTime());
        stages.add(connectingStage);
    }


}
