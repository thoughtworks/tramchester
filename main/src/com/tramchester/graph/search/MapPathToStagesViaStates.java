package com.tramchester.graph.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Platform;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.VehicleStage;
import com.tramchester.domain.transportStages.WalkingFromStationStage;
import com.tramchester.domain.transportStages.WalkingToStationStage;
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
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.TripRepository;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.graph.GraphPropertyKey.STATION_ID;
import static com.tramchester.graph.GraphPropertyKey.STOP_SEQ_NUM;
import static java.lang.String.format;

@LazySingleton
public class MapPathToStagesViaStates implements PathToStages {

    // TODO INFO to DEBUG, tidy up logging here
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

        MapStatesToPath mapStatesToPath = new MapStatesToPath(stationRepository, platformRepository, tripRepository, queryTime, mapper);

        TraversalState previous = new NotStartedState(traversalOps, stateFactory);

        int lastRelationshipCost = 0;
        for (Entity entity : path) {
            if (entity instanceof Relationship) {
                Relationship relationship = (Relationship) entity;
                logger.debug("Seen " + relationship.getType().name());
                lastRelationshipCost = nodeContentsRepository.getCost(relationship);
                if (lastRelationshipCost > 0) {
                    int total = previous.getTotalCost() + lastRelationshipCost;
                    mapStatesToPath.updateJourneyClock(total);
                }
                if (relationship.hasProperty(STOP_SEQ_NUM.getText())) {
                    mapStatesToPath.passStop(relationship);
                }
            } else {
                Node node = (Node) entity;
                Set<GraphBuilder.Labels> labels = GraphBuilder.Labels.from(node.getLabels());
                TraversalState next = previous.nextState(labels, node, mapStatesToPath, lastRelationshipCost);
                logger.debug("At state " + previous.getClass().getSimpleName() + " next is " + next.getClass().getSimpleName());
                previous = next;
            }
        }
        previous.toDestination(previous, path.endNode(), 0, mapStatesToPath);

        return mapStatesToPath.getStages();
    }


    private static class MapStatesToPath implements JourneyStateUpdate {

        private final CompositeStationRepository stationRepository;
        private final PlatformRepository platformRepository;
        private final TripRepository tripRepository;
        private ArrayList<Integer> stopSequenceNumbers;
        private final List<TransportStage<?, ?>> stages;
        private final TramTime queryTime;

        private IdFor<Station> actionStationId;
        private IdFor<Trip> tripId;
        private IdFor<Platform> boardingPlatformId;
        private TramTime actionTime;
        private int costAtBoarding;
        private int journeyClock;
        private int beginWalkClock;
        private LatLong walkStart;
        private final ObjectMapper mapper;

        public MapStatesToPath(CompositeStationRepository stationRepository, PlatformRepository platformRepository,
                               TripRepository tripRepository, TramTime queryTime, ObjectMapper mapper) {
            this.stationRepository = stationRepository;
            this.platformRepository = platformRepository;
            this.tripRepository = tripRepository;
            this.queryTime = queryTime;
            this.mapper = mapper;
            stages = new ArrayList<>();
            journeyClock = 0;
        }

        @Override
        public void board(TransportMode transportMode, Node node, boolean hasPlatform) throws TramchesterException {
            actionStationId = GraphProps.getStationId(node);
            logger.info("Board " + transportMode + " at " + actionStationId) ;
            if (hasPlatform) {
                boardingPlatformId = GraphProps.getPlatformIdFrom(node);
            }
            stopSequenceNumbers = new ArrayList<>();
        }

        @Override
        public void leave(TransportMode mode, int totalCost, Node routeStationNode) throws TramchesterException {
            if (actionTime ==null) {
                throw new RuntimeException("Not boarded yet");
            }
            IdFor<Station> lastStationId = GraphProps.getStationId(routeStationNode);
            logger.info("Leave " + mode + " at " + lastStationId + "  total cost = " + totalCost);
            Station firstStation = stationRepository.getStationById(actionStationId);
            Station lastStation = stationRepository.getStationById(lastStationId);

            Trip trip = tripRepository.getTripById(tripId);
            removeDestinationFrom(stopSequenceNumbers, trip, lastStationId);

            addVehicleStage(mode, totalCost, firstStation, lastStation, trip);

            reset();
        }

        protected void passStop(Relationship fromMinuteNodeRelationship) {
            logger.debug("pass stop");
            if (actionTime != null) {
                int stopSequenceNumber = GraphProps.getStopSequenceNumber(fromMinuteNodeRelationship);
                stopSequenceNumbers.add(stopSequenceNumber);
            }
        }

        @Override
        public void updateJourneyClock(int totalCost) {
            logger.debug("Update journey clock " + totalCost);
            journeyClock = totalCost;
            // noop
        }

        @Override
        public void recordTime(TramTime time, int totalCost) throws TramchesterException {
            logger.debug("Record time " + time + " total cost:" + totalCost);
            if (actionTime ==null) {
                logger.info("Boarding time set to " + time);
                this.actionTime = time;
                costAtBoarding = totalCost;
            }
        }

        @Override
        public void beginTrip(IdFor<Trip> newTripId) {
            logger.debug("Begin trip:" + newTripId);
            this.tripId = newTripId;
        }

        @Override
        public void beginWalk(Node beforeWalkNode, boolean atStart, int cost) {
            logger.info("Walk cost " + cost);
            if (atStart) {
                walkStart = GraphProps.getLatLong(beforeWalkNode);
                beginWalkClock = journeyClock;
                //actionTime = queryTime.plusMinutes(journeyClock);
                logger.info("Begin walk from start " + walkStart + " at " + queryTime.plusMinutes(beginWalkClock));
                actionStationId = null;
            } else {
                beginWalkClock = journeyClock - cost;
                actionStationId = GraphProps.getStationId(beforeWalkNode);
                //actionTime = queryTime.plusMinutes(journeyClock).minusMinutes(cost);
                logger.info("Begin walk from station " + actionStationId + " at " + queryTime.plusMinutes(beginWalkClock));
            }
        }

        @Override
        public void endWalk(Node endWalkNode, boolean atDestination) {

            if (actionStationId == null) {
                boolean atStation = GraphProps.hasProperty(STATION_ID, endWalkNode);
                if (atStation) {
                    MyLocation start = MyLocation.create(mapper, walkStart);
                    IdFor<Station> destinationStationId = GraphProps.getStationId(endWalkNode);
                    Station destination = stationRepository.getStationById(destinationStationId);
                    int duration = journeyClock - beginWalkClock;
                    logger.info("End walk to station " + destinationStationId + " duration " + duration);
                    WalkingToStationStage stage = new WalkingToStationStage(start, destination, duration, actionTime);
                    stages.add(stage);
                } else {
                    throw new RuntimeException("Ended walked at unexpected node " + endWalkNode.getAllProperties());
                }
            } else {
                LatLong walkEnd = GraphProps.getLatLong(endWalkNode);
                MyLocation destination = MyLocation.create(mapper, walkEnd);
                Station start = stationRepository.getStationById(actionStationId);
                int duration = journeyClock - beginWalkClock;
                logger.info("End walk from station to " +walkEnd + " duration " + duration);
                WalkingFromStationStage stage = new WalkingFromStationStage(start, destination, duration, actionTime);
                stages.add(stage);
            }

            reset();
        }

        public List<TransportStage<?, ?>> getStages() {
            return stages;
        }

        private void reset() {
            stopSequenceNumbers = null;
            boardingPlatformId = null;
            actionStationId = null;
            actionTime = null;
            costAtBoarding = Integer.MIN_VALUE;
            beginWalkClock = Integer.MIN_VALUE;
            walkStart = null;
        }

        private void addVehicleStage(TransportMode mode, int totalCost, Station firstStation, Station lastStation, Trip trip) {
            final VehicleStage vehicleStage = new VehicleStage(firstStation, trip.getRoute(), mode, trip,
                    actionTime, lastStation, stopSequenceNumbers);
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
