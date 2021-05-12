package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Platform;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.VehicleStage;
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
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import org.apache.commons.collections.EnumerationUtils;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.graph.GraphPropertyKey.STOP_SEQ_NUM;
import static java.lang.String.format;

@LazySingleton
public class MapPathToStagesViaStates implements PathToStages {
    private static final Logger logger = LoggerFactory.getLogger(MapPathToStagesViaStates.class);

    private final StationRepository stationRepository;
    private final PlatformRepository platformRepository;
    private final TraversalStateFactory stateFactory;
    private final NodeContentsRepository nodeContentsRepository;
    private final TripRepository tripRepository;
    private final SortsPositions sortsPosition;
    private final GraphQuery graphQuery;
    private final GraphDatabase graphDatabase;

    @Inject
    public MapPathToStagesViaStates(StationRepository stationRepository, PlatformRepository platformRepository, TraversalStateFactory stateFactory,
                                    NodeContentsRepository nodeContentsRepository,
                                    TripRepository tripRepository, SortsPositions sortsPosition, GraphQuery graphQuery,
                                    GraphDatabase graphDatabase) {
        this.stationRepository = stationRepository;
        this.platformRepository = platformRepository;
        this.stateFactory = stateFactory;
        this.nodeContentsRepository = nodeContentsRepository;
        this.tripRepository = tripRepository;
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
        TraversalOps traversalOps = new TraversalOps(nodeContentsRepository, tripRepository, sortsPosition, endStations,
                destinationNodeIds, destinationLatLon);

        MapState mapState = new MapState(stationRepository, platformRepository, tripRepository);

        TraversalState previous = new NotStartedState(traversalOps, stateFactory);

        int lastRelationshipCost = 0;
        for (Entity entity : path) {
            if (entity instanceof Relationship) {
                Relationship relationship = (Relationship) entity;
                lastRelationshipCost = nodeContentsRepository.getCost(relationship);
                if (lastRelationshipCost > 0) {
                    int total = previous.getTotalCost() + lastRelationshipCost;
                    mapState.updateJourneyClock(total);
                }
                if (relationship.hasProperty(STOP_SEQ_NUM.getText())) {
                    mapState.passStop(relationship);
                }
            } else {
                Node node = (Node) entity;
                Set<GraphBuilder.Labels> labels = GraphBuilder.Labels.from(node.getLabels());
                TraversalState next = previous.nextState(labels, node, mapState, lastRelationshipCost);
                previous = next;
            }

        }

        return mapState.getStages();
    }


    private static class MapState implements JourneyStateUpdate {

        private final StationRepository stationRepository;
        private final PlatformRepository platformRepository;
        private final TripRepository tripRepository;
        private ArrayList<Integer> stopSequenceNumbers;
        private final List<TransportStage<?, ?>> stages;

        private IdFor<Station> boardingStationId;
        private IdFor<Trip> tripId;
        private IdFor<Platform> boardingPlatformId;
        private TramTime boardingTime;
        private int costAtBoarding;

        public MapState(StationRepository stationRepository, PlatformRepository platformRepository, TripRepository tripRepository) {
            this.stationRepository = stationRepository;
            this.platformRepository = platformRepository;
            this.tripRepository = tripRepository;
            stages = new ArrayList<>();
        }

        @Override
        public void board(TransportMode transportMode, Node node, boolean hasPlatform) throws TramchesterException {
            logger.info("Board " + transportMode);
            boardingStationId = GraphProps.getStationId(node);
            if (hasPlatform) {
                boardingPlatformId = GraphProps.getPlatformIdFrom(node);
            }
            stopSequenceNumbers = new ArrayList<>();
        }

        @Override
        public void leave(TransportMode mode, int totalCost, Node routeStationNode) throws TramchesterException {
            logger.info("Leave " + mode + " total cost = " + totalCost);
            if (boardingTime==null) {
                throw new RuntimeException("Not boarded yet");
            }
            IdFor<Station> lastStationId = GraphProps.getStationId(routeStationNode);
            Station firstStation = stationRepository.getStationById(boardingStationId);
            Station lastStation = stationRepository.getStationById(lastStationId);

            Trip trip = tripRepository.getTripById(tripId);
            removeDestinationFrom(stopSequenceNumbers, trip, lastStationId);

            addVehicleStage(mode, totalCost, firstStation, lastStation, trip);

            reset();
        }


        protected void passStop(Relationship fromMinuteNodeRelationship) {
            logger.info("pass stop");
            if (boardingTime != null) {
                int stopSequenceNumber = GraphProps.getStopSequenceNumber(fromMinuteNodeRelationship);
                stopSequenceNumbers.add(stopSequenceNumber);
            }
        }

        @Override
        public void walkingConnection() {
            // TODO
        }

        @Override
        public void updateJourneyClock(int cost) {
            logger.info("Update journey clock " + cost);
            // noop
        }

        @Override
        public void recordTime(TramTime time, int totalCost) throws TramchesterException {
            logger.info("Record time " + time + " total cost:" + totalCost);
            if (boardingTime==null) {
                logger.info("Boarding time set to " + time);
                this.boardingTime = time;
                costAtBoarding = totalCost;
            }
        }

        @Override
        public void beginTrip(IdFor<Trip> newTripId) {
            logger.info("Begin trip:" + newTripId);
            this.tripId = newTripId;
        }

        public List<TransportStage<?, ?>> getStages() {
            return stages;
        }

        private void reset() {
            stopSequenceNumbers = null;
            boardingPlatformId = null;
            boardingStationId = null;
            boardingTime = null;
            costAtBoarding = Integer.MIN_VALUE;
        }

        private void addVehicleStage(TransportMode mode, int totalCost, Station firstStation, Station lastStation, Trip trip) {
            final VehicleStage vehicleStage = new VehicleStage(firstStation, trip.getRoute(), mode, trip,
                    boardingTime, lastStation, stopSequenceNumbers);
            vehicleStage.setCost(totalCost - costAtBoarding);
            if (boardingPlatformId!=null) {
                final Optional<Platform> platformById = platformRepository.getPlatformById(boardingPlatformId);
                platformById.ifPresent(vehicleStage::setPlatform);
            }
            stages.add(vehicleStage);
        }

        private void removeDestinationFrom(ArrayList<Integer> stopSequenceNumbers, Trip trip, IdFor<Station> lastStationId) {
            if (stopSequenceNumbers.isEmpty()) {
                return;
            }
            int lastIndex = stopSequenceNumbers.size()-1;
            int lastSeq = stopSequenceNumbers.get(lastIndex);
            StopCall finalPassed = trip.getStopCalls().getStopBySequenceNumber(lastSeq);
            if (finalPassed.getStationId().equals(lastStationId)) {
                stopSequenceNumbers.remove(lastIndex);
            }
        }
    }


}
