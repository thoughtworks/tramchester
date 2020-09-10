package com.tramchester.graph.search;


import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.MyLocationFactory;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;

public class MapPathToStages {
    private static final Logger logger = LoggerFactory.getLogger(MapPathToStages.class);

    private final TransportData transportData;
    private final MyLocationFactory myLocationFactory;
    private final PlatformRepository platformRepository;

    public MapPathToStages(TransportData transportData,
                           MyLocationFactory myLocationFactory, PlatformRepository platformRepository) {
        this.transportData = transportData;
        this.myLocationFactory = myLocationFactory;
        this.platformRepository = platformRepository;
    }

    public List<TransportStage<?,?>> mapDirect(RouteCalculator.TimedPath timedPath, JourneyRequest journeyRequest) {
        Path path = timedPath.getPath();
        TramTime queryTime = timedPath.getQueryTime();
        logger.info(format("Mapping path length %s to transport stages for %s at %s",
                path.length(), journeyRequest, queryTime));
        ArrayList<TransportStage<?,?>> results = new ArrayList<>();

        List<Relationship> relationships = new ArrayList<>();
        path.relationships().forEach(relationships::add);

        if (relationships.size()==0) {
            return results;
        }
        if (relationships.size()==1) {
            // ASSUME: direct walk
            results.add(directWalk(relationships.get(0), queryTime));
            return results;
        }

        State state = new State(transportData, platformRepository, queryTime);
        for(Relationship relationship : relationships) {
            TransportRelationshipTypes type = TransportRelationshipTypes.valueOf(relationship.getType().name());
            logger.debug("Mapping type " + type);

            switch (type) {
                case BOARD:
                case INTERCHANGE_BOARD:
                    state.board(relationship);
                    break;
                case DEPART:
                case INTERCHANGE_DEPART:
                    results.add(state.depart(relationship));
                    break;
                case TO_MINUTE:
                    state.beginTrip(relationship).ifPresent(results::add);
                    break;
                case TRAM_GOES_TO:
                case BUS_GOES_TO:
                case TRAIN_GOES_TO:
                    state.passStop(relationship);
                    break;
                case WALKS_TO:
                    state.walk(relationship);
                    break;
                case WALKS_FROM:
                    results.add(state.walkFrom(relationship));
                    break;
                case TO_SERVICE:
//                    state.toService(relationship);
                    break;
                case ENTER_PLATFORM:
                    state.enterPlatform(relationship);
                    break;
                case LEAVE_PLATFORM:
                    state.leavePlatform(relationship);
                    break;
                case TO_HOUR:
                case FINISH_WALK:
                    break;
                case BUS_NEIGHBOUR:
                case TRAM_NEIGHBOUR:
                case TRAIN_NEIGHBOUR:
                    results.add(state.walkBetween(relationship));
                    break;
                default:
                    throw new RuntimeException(format("Unexpected relationship %s in path %s", type, path));
            }
        }
        String msg = format("Mapped path length %s to transport %s stages for %s", path.length(), results.size(), journeyRequest);
        if (results.isEmpty()) {
            logger.warn(msg);
        } else {
            logger.info(msg);
        }
        return results;
    }

    private TransportStage<?,?> directWalk(Relationship relationship, TramTime timeWalkStarted) {
        if (relationship.isType(WALKS_TO)) {
            WalkStarted walkStarted = walkStarted(relationship);
            return new WalkingToStationStage(walkStarted.start, walkStarted.destination,
                    walkStarted.cost, timeWalkStarted);
        } else if (relationship.isType(WALKS_FROM)) {
            return createWalkFrom(relationship, timeWalkStarted);
        }
        else if (relationship.isType(BUS_NEIGHBOUR) ||
                relationship.isType(TRAM_NEIGHBOUR) ||
                relationship.isType(TRAIN_NEIGHBOUR) ) {
            return createWalkFromNeighbour(relationship, timeWalkStarted);
        }
        else {
            throw new RuntimeException("Unexpected single relationship: " +relationship);
        }
    }

    private int getCost(Relationship relationship) {
        return GraphProps.getCost(relationship);
    }

    private WalkStarted walkStarted(Relationship relationship) {
        // position -> station
        int cost = getCost(relationship);

        IdFor<Station> stationId = IdFor.getStationIdFrom(relationship);
        Station destination = transportData.getStationById(stationId);

        Node startNode = relationship.getStartNode();
        LatLong latLong = GraphProps.getLatLong(startNode);
        MyLocation start = myLocationFactory.create(latLong);
        return new WalkStarted(start, destination, cost);
    }

    private WalkingFromStationStage createWalkFrom(Relationship relationship, TramTime walkStartTime) {
        // station -> position
        int cost = getCost(relationship);

        IdFor<Station> stationId = IdFor.getStationIdFrom(relationship);
        Station start = transportData.getStationById(stationId);

        Node endNode = relationship.getEndNode();
        LatLong latLong = GraphProps.getLatLong(endNode);
        MyLocation walkEnd = myLocationFactory.create(latLong);

        return new WalkingFromStationStage(start, walkEnd, cost, walkStartTime);
    }

    private TransportStage createWalkFromNeighbour(Relationship relationship, TramTime walkStartTime) {
        // station -> station, neighbours...
        int cost = getCost(relationship);

        IdFor<Station> startStationId = IdFor.getStationIdFrom(relationship.getStartNode());
        Station start = transportData.getStationById(startStationId);

        IdFor<Station> endStationId = IdFor.getStationIdFrom(relationship.getEndNode());
        Station end = transportData.getStationById(endStationId);

        return new ConnectingStage(start, end, cost, walkStartTime);
    }

    private class State {
        private final TransportData transportData;
        private final PlatformRepository platformRepository;
        private final TramTime queryTime;

        private WalkStarted walkStarted;
        private TramTime boardingTime;
        private TramTime departTime;
        private Station boardingStation;
        private IdFor<Route> routeCode;
        private IdFor<Trip> tripId;
        private int stopsSeen;
        private int tripCost;
        private Optional<Platform> boardingPlatform;
        private Route route;

        private int boardCost;
        private int platformEnterCost;
        private int platformLeaveCost;
        private int departCost;

        private State(TransportData transportData, PlatformRepository platformRepository, TramTime queryTime) {
            this.transportData = transportData;
            this.platformRepository = platformRepository;
            this.queryTime = queryTime;
            reset();
        }

        protected void board(Relationship relationship) {
            boardCost = getCost(relationship);
            boardingStation = transportData.getStationById(IdFor.getStationIdFrom(relationship));
            routeCode = IdFor.getRouteIdFrom(relationship);
            route = transportData.getRouteById(routeCode);
            if (boardingStation.hasPlatforms()) {
                IdFor<Platform> platformId = IdFor.getPlatformIdFrom(relationship);
                boardingPlatform = platformRepository.getPlatformById(platformId);
            }
            departTime = null;
        }

        protected VehicleStage depart(Relationship relationship) {
            IdFor<Station> stationId = IdFor.getStationIdFrom(relationship);
            Station departStation = transportData.getStationById(stationId);
            Trip trip = transportData.getTripById(tripId);

            int passedStops = stopsSeen - 1;
            VehicleStage vehicleStage = new VehicleStage(boardingStation, route,
                    route.getTransportMode(), trip, boardingTime,
                    departStation, passedStops);

            if (stopsSeen == 0) {
                logger.error("Zero passed stops " + vehicleStage);
            }

            boardingPlatform.ifPresent(vehicleStage::setPlatform);
            vehicleStage.setCost(tripCost);
            reset();

            departCost = getCost(relationship);
            departTime = vehicleStage.getExpectedArrivalTime();

            return vehicleStage;
        }

        private void reset() {
            stopsSeen = 0; // don't count single stops
            tripId = IdFor.invalid();
            routeCode = IdFor.invalid();
            tripCost = 0;
            boardingPlatform = Optional.empty();
        }

        protected Optional<WalkingToStationStage> beginTrip(Relationship relationship) {
            IdFor<Trip> newTripId = IdFor.getTripIdFrom(relationship);

            if (tripId.notValid()) {
                this.tripId = newTripId;
                LocalTime property = GraphProps.getTime(relationship);
                boardingTime = TramTime.of(property);
            } else if (!tripId.equals(newTripId)){
                throw new RuntimeException(format("Mid flight change of trip from %s to %s", tripId, newTripId));
            }

            if (walkStarted==null) {
                return Optional.empty();
            }

            int totalCostOfWalk = walkStarted.cost + boardCost + platformEnterCost;
            TramTime timeWalkStarted = boardingTime.minusMinutes(totalCostOfWalk);
            if (timeWalkStarted.isBefore(queryTime)) {
                logger.error("Computed walk start ahead of query time");
            }
            WalkingToStationStage walkingStage = new WalkingToStationStage(walkStarted.start, walkStarted.destination,
                    walkStarted.cost, timeWalkStarted);
            walkStarted = null;
            return Optional.of(walkingStage);
        }

        protected void passStop(Relationship relationship) {
            tripCost = tripCost + getCost(relationship);
            stopsSeen = stopsSeen + 1;
        }

        protected void walk(Relationship relationship) {
            walkStarted = walkStarted(relationship);
        }


        protected TransportStage walkBetween(Relationship betweenStations) {
            TramTime walkStartTime;
            if (departTime==null) {
                walkStartTime = queryTime.plusMinutes(platformLeaveCost + departCost);
            } else {
                walkStartTime = departTime.plusMinutes(platformLeaveCost + departCost);
            }
            return createWalkFromNeighbour(betweenStations, walkStartTime);
        }

        protected void enterPlatform(Relationship relationship) {
            platformEnterCost = getCost(relationship);
        }

        protected WalkingFromStationStage walkFrom(Relationship relationship) {
            TramTime walkStartTime;
            if (departTime==null) {
                walkStartTime = queryTime.plusMinutes(platformLeaveCost + departCost);
            } else {
                walkStartTime = departTime.plusMinutes(platformLeaveCost + departCost);
            }
            return createWalkFrom(relationship, walkStartTime);
        }

        protected void leavePlatform(Relationship relationship) {
            platformLeaveCost = getCost(relationship);
        }

    }

    private static class WalkStarted {
        private final MyLocation start;
        private final Station destination;
        private final int cost;

        protected WalkStarted(MyLocation start, Station destination, int cost) {

            this.start = start;
            this.destination = destination;
            this.cost = cost;
        }
    }

}
