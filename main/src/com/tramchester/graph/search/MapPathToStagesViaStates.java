package com.tramchester.graph.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphBuilder;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.graph.GraphPropertyKey.STOP_SEQ_NUM;
import static java.lang.String.format;

@LazySingleton
public class MapPathToStagesViaStates implements PathToStages {

    private static final Logger logger = LoggerFactory.getLogger(MapPathToStagesViaStates.class);

    private final CompositeStationRepository stationRepository;
    private final PlatformRepository platformRepository;
    private final TraversalStateFactory stateFactory;
    private final NodeContentsRepository nodeContentsRepository;
    private final TripRepository tripRepository;
    private final SortsPositions sortsPosition;
    private final GraphQuery graphQuery;
    private final GraphDatabase graphDatabase;
    private final ObjectMapper mapper;

    @Inject
    public MapPathToStagesViaStates(CompositeStationRepository stationRepository, PlatformRepository platformRepository,
                                    TraversalStateFactory stateFactory, NodeContentsRepository nodeContentsRepository,
                                    TripRepository tripRepository, SortsPositions sortsPosition, GraphQuery graphQuery,
                                    GraphDatabase graphDatabase, ObjectMapper mapper) {
        this.stationRepository = stationRepository;
        this.platformRepository = platformRepository;
        this.stateFactory = stateFactory;
        this.nodeContentsRepository = nodeContentsRepository;
        this.tripRepository = tripRepository;
        this.sortsPosition = sortsPosition;
        this.graphQuery = graphQuery;
        this.graphDatabase = graphDatabase;
        this.mapper = mapper;
    }

    private Set<Long> getDestinationNodeIds(Set<Station> endStations) {
        Set<Long> destinationNodeIds;
        try(Transaction txn = graphDatabase.beginTx()) {
            destinationNodeIds = endStations.stream().
                    map(station -> graphQuery.getStationOrGrouped(txn, station)).
                    filter(Objects::nonNull).
                    map(Entity::getId).
                    collect(Collectors.toSet());
        }
        if (endStations.size()!=destinationNodeIds.size()) {
            logger.error("Could not find destination node ids for all end stations (is the graph filtered?)");
            try(Transaction txn = graphDatabase.beginTx()) {
                IdSet<Station> noNodeFound = endStations.stream().
                        filter(station -> graphQuery.getStationOrGrouped(txn, station) == null).
                        collect(IdSet.collector());
                logger.error("Missing nodes id for these desinations: " + noNodeFound);
            }
        }
        return destinationNodeIds;
    }

    @Override
    public List<TransportStage<?, ?>> mapDirect(RouteCalculator.TimedPath timedPath, JourneyRequest journeyRequest, Set<Station> endStations) {
        Path path = timedPath.getPath();
        TramTime queryTime = timedPath.getQueryTime();
        logger.info(format("Mapping path length %s to transport stages for %s at %s with %s changes",
                path.length(), journeyRequest, queryTime, timedPath.getNumChanges()));

        LatLong destinationLatLon = sortsPosition.midPointFrom(endStations);

        Set<Long> destinationNodeIds = getDestinationNodeIds(endStations);
        TraversalOps traversalOps = new TraversalOps(nodeContentsRepository, tripRepository, sortsPosition, endStations,
                destinationNodeIds, destinationLatLon);

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

        return mapStatesToStages.getStages();
    }


}
