package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.TraversalState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

@LazySingleton
public class MapPathToStagesViaStates implements PathToStages {
    private static final Logger logger = LoggerFactory.getLogger(MapPathToStagesViaStates.class);

    private final StationRepository stationRepository;
    private final TraversalStateFactory stateFactory;
    private final NodeContentsRepository nodeContentsRepository;
    private final TripRepository tripRespository;
    private final SortsPositions sortsPosition;
    private final GraphQuery graphQuery;
    private final GraphDatabase graphDatabase;

    @Inject
    public MapPathToStagesViaStates(StationRepository stationRepository, TraversalStateFactory stateFactory, NodeContentsRepository nodeContentsRepository,
                                    TripRepository tripRespository, SortsPositions sortsPosition, GraphQuery graphQuery,
                                    GraphDatabase graphDatabase) {
        this.stationRepository = stationRepository;
        this.stateFactory = stateFactory;
        this.nodeContentsRepository = nodeContentsRepository;
        this.tripRespository = tripRespository;
        this.sortsPosition = sortsPosition;
        this.graphQuery = graphQuery;
        this.graphDatabase = graphDatabase;
    }


    private Set<Long> getDestinationNodeIds(Set<Station> endStations) {
        Set<Long> destinationNodeIds;
        try(Transaction txn = graphDatabase.beginTx()) {
            destinationNodeIds = endStations.stream().
                    map(station -> graphQuery.getStationOrGrouped(txn, station)).
                    map(Entity::getId).
                    collect(Collectors.toSet());
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
        TraversalOps traversalOps = new TraversalOps(nodeContentsRepository, tripRespository, sortsPosition, endStations,
                destinationNodeIds, destinationLatLon);

        List<TransportStage<?, ?>> result = new ArrayList<>();
        JourneyStateUpdate journeyState = new MapState(stationRepository, queryTime);

        TraversalState previous = new NotStartedState(traversalOps, stateFactory);

        for(Node node : path.nodes()) {
            int cost = 0;
            if (path.lastRelationship()!=null) {
                cost = nodeContentsRepository.getCost(path.lastRelationship());
                if (cost>0) {
                    int total = previous.getTotalCost() + cost;
                    journeyState.updateJourneyClock(total);
                }
            }

            Set<GraphBuilder.Labels> labels = GraphBuilder.Labels.from(node.getLabels());
            TraversalState next = previous.nextState(labels, node, journeyState, cost);
            previous = next;
        }

        return result;
    }


    private static class MapState implements JourneyStateUpdate {

        private final StationRepository stationRepository;
        private final TramTime queryTime;
        private TramTime departFirstStationTime;
        private IdFor<Station> boardingStationId;

        public MapState(StationRepository stationRepository, TramTime queryTime) {
            this.stationRepository = stationRepository;
            this.queryTime = queryTime;
        }

        @Override
        public void board(TransportMode transportMode, Node node) throws TramchesterException {
            boardingStationId = GraphProps.getStationId(node);
        }

        @Override
        public void leave(TransportMode mode, int totalCost) throws TramchesterException {
            Station firstStation = stationRepository.getStationById(boardingStationId);
//            new VehicleStage(firstStation, route, mode, trip, departFirstStationTime, lastStations, stopSequenceNumbers, hasPlatform);
        }

        @Override
        public void walkingConnection() {

        }

        @Override
        public void updateJourneyClock(int total) {

        }

        @Override
        public void recordVehicleDetails(TramTime time, int totalCost) throws TramchesterException {

        }
    }


}
