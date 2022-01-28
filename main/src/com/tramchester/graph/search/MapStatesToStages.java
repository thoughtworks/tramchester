package com.tramchester.graph.search;

import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.*;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.repository.*;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.tramchester.graph.GraphPropertyKey.STATION_ID;

class MapStatesToStages implements JourneyStateUpdate {
    private static final Logger logger = LoggerFactory.getLogger(MapStatesToStages.class);

    private final StationRepository stationRepository;
    private final PlatformRepository platformRepository;
    private final TripRepository tripRepository;
    private final List<TransportStage<?, ?>> stages;

    private boolean onVehicle;
    private IdFor<Trip> tripId;

    private int totalCost; // total cost of entire journey
    private TramTime actualTime; // updated each time pass minute node and know 'actual' time
    private int costOffsetAtActual; // total cost at point got 'actual' time update

    @Deprecated
    private TramTime boardingTime;

    @Deprecated
    private TramTime beginWalkClock;

    private IdFor<Station> walkStartStation;

    private WalkFromStartPending walkFromStartPending;
    private VehicleStagePending vehicleStagePending;


    public MapStatesToStages(StationRepository stationRepository, PlatformRepository platformRepository,
                             TripRepository tripRepository, TramTime queryTime) {
        this.stationRepository = stationRepository;
        this.platformRepository = platformRepository;
        this.tripRepository = tripRepository;

        actualTime = queryTime;
        stages = new ArrayList<>();
        onVehicle = false;
        totalCost = 0;
    }

    @Override
    public void board(TransportMode transportMode, Node node, boolean hasPlatform) {
        onVehicle = true;
        boardingTime = null;

        IdFor<Station> actionStationId = GraphProps.getStationId(node);
        logger.info("Board " + transportMode + " " + actionStationId + " totalcost  " + totalCost);
        vehicleStagePending = new VehicleStagePending(stationRepository, tripRepository, platformRepository,
                actionStationId, totalCost);
        if (hasPlatform) {
            IdFor<Platform> boardingPlatformId = GraphProps.getPlatformIdFrom(node);
            vehicleStagePending.addPlatform(boardingPlatformId);
        }
    }

    @Override
    public void recordTime(TramTime time, int totalCost) {
        logger.debug("Record actual time " + time + " total cost:" + totalCost);
        this.actualTime = time;
        costOffsetAtActual = totalCost;
        if (onVehicle && boardingTime == null) {
            vehicleStagePending.setBoardingTime(actualTime);
            boardingTime = time;
        }
        if (walkFromStartPending != null) {
            WalkingToStationStage walkingToStationStage = walkFromStartPending.createStage(actualTime, totalCost);
            logger.info("Add " + walkingToStationStage);
            stages.add(walkingToStationStage);
            walkFromStartPending = null;
        }
    }

    @Override
    public void leave(TransportMode mode, int totalCost, Node routeStationNode) {
        if (!onVehicle) {
            throw new RuntimeException("Not on vehicle");
        }
        onVehicle = false;

        final VehicleStage vehicleStage = vehicleStagePending.createStage(routeStationNode, totalCost, tripId, mode);
        stages.add(vehicleStage);
        logger.info("Added " + vehicleStage);
        reset();
    }

    protected void passStop(Relationship fromMinuteNodeRelationship) {
        logger.debug("pass stop");
        if (onVehicle) {
            int stopSequenceNumber = GraphProps.getStopSequenceNumber(fromMinuteNodeRelationship);
            vehicleStagePending.addStopSeqNumber(stopSequenceNumber);
        } else {
            logger.error("Passed stop but not on vehicle");
        }
    }

    @Override
    public void updateTotalCost(int totalCost) {
        logger.debug("Update total journeyCost " + totalCost);
        this.totalCost = totalCost;
        logger.debug("Actual clock " + getActualClock());
    }

    private TramTime getActualClock() {
        return actualTime.plusMinutes(totalCost - costOffsetAtActual);
    }

    @Override
    public void beginTrip(IdFor<Trip> newTripId) {
        logger.debug("Begin trip:" + newTripId);
        this.tripId = newTripId;
    }

    @Override
    public void beginWalk(Node beforeWalkNode, boolean atStart, int cost) {
        logger.debug("Walk cost " + cost);
        if (atStart) {
            LatLong walkStartLocation = GraphProps.getLatLong(beforeWalkNode);
            walkFromStartPending = new WalkFromStartPending(walkStartLocation);
            walkStartStation = null;
            beginWalkClock = getActualClock();
            logger.info("Begin walk from start " + walkStartLocation);
        } else {
            walkStartStation = GraphProps.getStationId(beforeWalkNode);
            beginWalkClock = getActualClock().minusMinutes(cost);
            logger.info("Begin walk from station " + walkStartStation + " at " + beginWalkClock);
        }
    }

    @Override
    public void endWalk(Node endWalkNode) {

        int duration = TramTime.diffenceAsMinutes(beginWalkClock, getActualClock());

        if (walkFromStartPending != null) {
            boolean atStation = GraphProps.hasProperty(STATION_ID, endWalkNode);
            if (atStation) {
                IdFor<Station> destinationStationId = GraphProps.getStationId(endWalkNode);
                Station destination = stationRepository.getStationById(destinationStationId);
                walkFromStartPending.setDestinationAndDuration(totalCost, destination, duration);
            }  else {
                throw new RuntimeException("Ended walked at unexpected node " + endWalkNode.getAllProperties());
            }
        } else {
            if (walkStartStation!=null) {
                // walk from a station
                Station walkStation = stationRepository.getStationById(walkStartStation);
                LatLong walkEnd = GraphProps.getLatLong(endWalkNode);
                MyLocation destination = MyLocation.create(walkEnd);

                logger.info("End walk from station to " + walkEnd + " duration " + duration);
                WalkingFromStationStage stage = new WalkingFromStationStage(walkStation, destination,
                        duration, beginWalkClock);
                stages.add(stage);
            } else {
                throw new RuntimeException("Unexpected end of walk not form a station");
            }
        }

        reset();
    }

    @Override
    public void toNeighbour(Node startNode, Node endNode, int cost) {
        IdFor<Station> startId = GraphProps.getStationId(startNode);
        IdFor<Station> endId = GraphProps.getStationId(endNode);
        Station start = stationRepository.getStationById(startId);
        Station end = stationRepository.getStationById(endId);
        ConnectingStage<Station,Station> connectingStage = new ConnectingStage<>(start, end, cost, getActualClock());
        logger.info("Added stage " + connectingStage);
        stages.add(connectingStage);
    }

    @Override
    public void seenStation(IdFor<Station> stationId) {
        // noop
    }

    public List<TransportStage<?, ?>> getStages() {
        if (walkFromStartPending != null) {
            WalkingStage<?,?> walkingStage = walkFromStartPending.createStage(getActualClock(), totalCost);
            logger.info("Add final pending walking stage " + walkingStage);
            stages.add(walkingStage);
        }
        return stages;
    }

    private void reset() {
        beginWalkClock = null;
    }

    private static class WalkFromStartPending {

        private final LatLong walkStart;
        private int totalCostAtDestination;
        private Station destination;
        private int duration;

        public WalkFromStartPending(LatLong walkStart) {
            this.walkStart = walkStart;
        }

        public void setDestinationAndDuration(int totalCost, Station destination, int duration) {
            totalCostAtDestination = totalCost;
            this.destination = destination;
            this.duration = duration;
        }

        public WalkingToStationStage createStage(TramTime actualTime, int totalCostNow) {
            MyLocation walkStation = MyLocation.create(walkStart);
            logger.info("End walk to station " + destination.getId() + " duration " + duration);

            // offset for boarding cost
            int offset = totalCostNow - totalCostAtDestination;

            TramTime walkStartTime = actualTime.minusMinutes(duration+offset);
            return new WalkingToStationStage(walkStation, destination, duration, walkStartTime);
        }
    }

    private static class VehicleStagePending {

        private final StationRepositoryPublic stationRepository;
        private final TripRepository tripRepository;
        private final PlatformRepository platformRepository;

        private final ArrayList<Integer> stopSequenceNumbers;
        private final IdFor<Station> actionStationId;

        private final int costOffsetAtBoarding;
        private TramTime boardingTime;
        private IdFor<Platform> boardingPlatformId;

        public VehicleStagePending(StationRepositoryPublic stationRepository, TripRepository tripRepository,
                                   PlatformRepository platformRepository,
                                   IdFor<Station> actionStationId, int costOffsetAtBoarding) {
            this.stationRepository = stationRepository;
            this.tripRepository = tripRepository;
            this.platformRepository = platformRepository;
            this.actionStationId = actionStationId;
            this.costOffsetAtBoarding = costOffsetAtBoarding;
            this.stopSequenceNumbers = new ArrayList<>();
            this.boardingTime = null;
        }

        public void addPlatform(IdFor<Platform> boardingPlatformId) {
            this.boardingPlatformId = boardingPlatformId;
        }

        public VehicleStage createStage(Entity routeStationNode, int totalCost, IdFor<Trip> tripId, TransportMode mode) {
            IdFor<Station> lastStationId = GraphProps.getStationId(routeStationNode);
            int cost = totalCost - costOffsetAtBoarding;

            logger.info("Leave " + mode + " at " + lastStationId + "  cost = " + cost);

            Station firstStation = stationRepository.getStationById(actionStationId);
            Station lastStation = stationRepository.getStationById(lastStationId);
            Trip trip = tripRepository.getTripById(tripId);
            removeDestinationFrom(stopSequenceNumbers, trip, lastStationId);

            final VehicleStage vehicleStage = new VehicleStage(firstStation, trip.getRoute(), mode, trip,
                    boardingTime, lastStation, stopSequenceNumbers);
            vehicleStage.setCost(cost);
            if (boardingPlatformId != null) {
                final Optional<Platform> platformById = platformRepository.getPlatformById(boardingPlatformId);
                platformById.ifPresent(vehicleStage::setPlatform);
            }

            return vehicleStage;
        }

        public void addStopSeqNumber(int stopSequenceNumber) {
            stopSequenceNumbers.add(stopSequenceNumber);
        }

        private void removeDestinationFrom(ArrayList<Integer> stopSequenceNumbers, Trip trip, IdFor<Station> lastStationId) {
            if (stopSequenceNumbers.isEmpty()) {
                return;
            }
            int lastIndex = stopSequenceNumbers.size() - 1;
            int lastJourneyStopsSequenceNumber = stopSequenceNumbers.get(lastIndex);
            StopCall finalPassed = trip.getStopCalls().getStopBySequenceNumber(lastJourneyStopsSequenceNumber);
            if (finalPassed.getStationId().equals(lastStationId)) {
                stopSequenceNumbers.remove(lastIndex);
            }
        }

        public void setBoardingTime(TramTime actualTime) {
            if (boardingTime==null) {
                boardingTime = actualTime;
            }
        }
    }
}
