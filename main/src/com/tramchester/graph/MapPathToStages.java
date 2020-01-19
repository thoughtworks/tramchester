package com.tramchester.graph;


import com.tramchester.domain.Station;
import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.tramchester.graph.GraphStaticKeys.*;
import static com.tramchester.graph.GraphStaticKeys.RouteStation.ROUTE_NAME;
import static java.lang.String.format;


public class MapPathToStages {
    private static final Logger logger = LoggerFactory.getLogger(MapPathToStages.class);

    private final RouteCodeToClassMapper routeIdToClass;
    private final TransportData transportData;
    private final MyLocationFactory myLocationFactory;
    private PlatformRepository platformRepository;

    public MapPathToStages(RouteCodeToClassMapper routeIdToClass, TransportData transportData,
                           MyLocationFactory myLocationFactory, PlatformRepository platformRepository) {
        this.routeIdToClass = routeIdToClass;
        this.transportData = transportData;
        this.myLocationFactory = myLocationFactory;
        this.platformRepository = platformRepository;
    }

    // TODO Use Traversal State from the path instead of the Path itself??
    public List<RawStage> mapDirect(WeightedPath path, TramTime queryTime) {
        ArrayList<RawStage> results = new ArrayList<>();

        List<Relationship> relationships = new ArrayList<>();
        path.relationships().forEach(relationships::add);

        if (relationships.size()==0) {
            return results;
        }
        if (relationships.size()==1) {
            // direct walk
            results.add(directWalk(relationships.get(0), queryTime));
            return results;
        }

        State state = new State(transportData, myLocationFactory, platformRepository);
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
                    state.passStop(relationship);
                    break;
                case WALKS_TO:
                    state.walk(relationship);
                    break;
                case TO_SERVICE:
//                    state.toService(relationship);
                    break;
                case ENTER_PLATFORM:
                    state.enterPlatform(relationship);
                    break;
                case LEAVE_PLATFORM:
                    break;
                case TO_HOUR:
                    break;
                default:
                    throw new RuntimeException("Unexpected relationship in path " + path.toString());
            }
        }
        return results;
    }

    private RawStage directWalk(Relationship relationships, TramTime timeWalkStarted) {
        WalkStarted walkStarted = walkStarted(relationships);
        return new RawWalkingStage(walkStarted.start, walkStarted.destination,
                walkStarted.cost, timeWalkStarted);
    }

    private int getCost(Relationship relationship) {
        return (int)relationship.getProperty(COST);
    }

    private WalkStarted walkStarted(Relationship relationship) {
        int cost = getCost(relationship);
        Node startNode = relationship.getStartNode();

        String stationId = relationship.getProperty(STATION_ID).toString();
        Location destination = transportData.getStation(stationId).get();

        double lat = (double)startNode.getProperty(GraphStaticKeys.Station.LAT);
        double lon =  (double)startNode.getProperty(GraphStaticKeys.Station.LONG);
        Location start = myLocationFactory.create(new LatLong(lat,lon));
        return new WalkStarted(start, destination, cost);
    }

    private class State {
        private final TransportData transportData;
        private final MyLocationFactory myLocationFactory;
        private final PlatformRepository platformRepository;

        private WalkStarted walkStarted;
        private TramTime boardingTime;
        private Station boardingStation;
        private String routeCode;
        private String tripId;
        private int passedStops;
        private int tripCost;
        private Optional<Platform> boardingPlatform;
        private String routeName;
        private int boardCost;
        private int platformCost;

        private State(TransportData transportData, MyLocationFactory myLocationFactory, PlatformRepository platformRepository) {
            this.transportData = transportData;
            this.myLocationFactory = myLocationFactory;
            this.platformRepository = platformRepository;
            reset();
        }

        public void board(Relationship relationship) {
            boardCost = getCost(relationship);
            boardingStation = transportData.getStation(relationship.getProperty(STATION_ID).toString()).get();
            routeCode = relationship.getProperty(ROUTE_ID).toString();
            routeName = relationship.getProperty(ROUTE_NAME).toString();
            String stopId = relationship.getProperty(PLATFORM_ID).toString();
            boardingPlatform = platformRepository.getPlatformById(stopId);
        }

        public RawVehicleStage depart(Relationship relationship) {
            String stationId = relationship.getProperty(STATION_ID).toString();
            Station departStation = transportData.getStation(stationId).get();
            Trip trip = transportData.getTrip(tripId);

            RawVehicleStage rawVehicleStage = new RawVehicleStage(boardingStation, routeName,
                    TransportMode.Tram, routeIdToClass.map(routeCode), trip);
            // TODO Into builder
            rawVehicleStage.setDepartTime(boardingTime);
            rawVehicleStage.setLastStation(departStation, passedStops);
            boardingPlatform.ifPresent(rawVehicleStage::setPlatform);
            rawVehicleStage.setCost(tripCost);

            reset();
            return rawVehicleStage;
        }

        private void reset() {
            passedStops = -1; // don't count single stops
            tripId = "";
            routeCode = "";
            tripId = "";
            tripCost = 0;
            boardingPlatform = Optional.empty();
        }

        public Optional<RawWalkingStage> beginTrip(Relationship relationship) {
            String newTripId = relationship.getProperty(TRIP_ID).toString();

            if (tripId.isEmpty()) {
                this.tripId = newTripId;
                LocalTime property = (LocalTime) relationship.getProperty(TIME);
                boardingTime = TramTime.of(property);
            } else if (!tripId.equals(newTripId)){
                throw new RuntimeException(format("Mid flight change of trip from %s to %s", tripId, newTripId));
            }

            if (walkStarted==null) {
                return Optional.empty();
            }

            int totalCostOfWalk = walkStarted.cost + boardCost + platformCost;
            TramTime timeWalkStarted = boardingTime.minusMinutes(totalCostOfWalk);
            RawWalkingStage walkingStage = new RawWalkingStage(walkStarted.start, walkStarted.destination,
                    walkStarted.cost, timeWalkStarted);
            walkStarted = null;
            return Optional.of(walkingStage);
        }

        public void passStop(Relationship relationship) {
            tripCost = tripCost + getCost(relationship);
            passedStops = passedStops + 1;
        }

        public void walk(Relationship relationship) {
            walkStarted = walkStarted(relationship);
        }

        public void enterPlatform(Relationship relationship) {
            platformCost = getCost(relationship);
        }
    }

    private class WalkStarted {

        private final Location start;
        private final Location destination;
        private final int cost;

        public WalkStarted(Location start, Location destination, int cost) {

            this.start = start;
            this.destination = destination;
            this.cost = cost;
        }
    }

}
