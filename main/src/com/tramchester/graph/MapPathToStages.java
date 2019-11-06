package com.tramchester.graph;


import com.tramchester.domain.*;
import com.tramchester.domain.Station;
import com.tramchester.graph.Relationships.PathToTransportRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.tramchester.graph.GraphStaticKeys.*;


public class MapPathToStages {
    private static final Logger logger = LoggerFactory.getLogger(MapPathToStages.class);

    private final PathToTransportRelationship pathToTransportRelationship;
    private final MapTransportRelationshipsToStages mapTransportRelationshipsToStages;
    private final RouteCodeToClassMapper routeIdToClass;
    private final StationRepository stationRepository;

    public MapPathToStages(PathToTransportRelationship pathToTransportRelationship, MapTransportRelationshipsToStages mapTransportRelationshipsToStages,
                           RouteCodeToClassMapper routeIdToClass, StationRepository stationRepository) {
        this.pathToTransportRelationship = pathToTransportRelationship;
        this.mapTransportRelationshipsToStages = mapTransportRelationshipsToStages;
        this.routeIdToClass = routeIdToClass;
        this.stationRepository = stationRepository;
    }

    public List<RawStage> map(WeightedPath path, LocalTime queryTime) {
        List<TransportRelationship> relationships = pathToTransportRelationship.mapPath(path);
        return mapTransportRelationshipsToStages.mapStages(relationships, queryTime);
    }

    public List<RawStage> mapDirect(WeightedPath path, LocalTime queryTime) {
        ArrayList<RawStage> results = new ArrayList<>();
        State state = new State(stationRepository);

        for(Relationship relationship : path.relationships()) {
            TransportRelationshipTypes type = TransportRelationshipTypes.valueOf(relationship.getType().name());
            logger.debug("Mapping type " + type);
            Node endNode = relationship.getEndNode();
            Node startNode = relationship.getStartNode();

            switch (type) {
                case BOARD:
                case INTERCHANGE_BOARD:
                    state.board(relationship);
                    break;
                case DEPART:
                case INTERCHANGE_DEPART:
                    results.add(state.getVehicleStage(relationship));
                    break;
                case TO_HOUR:
                    break;
                case TO_MINUTE:
                    state.beginTrip(relationship);
                    break;
                case TRAM_GOES_TO:
                    state.passStop(relationship);
                    break;
                case ENTER_PLATFORM:
                    break;
                case LEAVE_PLATFORM:
                    break;

                case WALKS_TO:
//                    state.beginWalk();
//                    state.endWalk(endNode);
                    break;
                case TO_SERVICE:
                    state.toService(relationship);
                    break;

                default:
                    throw new RuntimeException("Unexpected relationship in path " + path.toString());

            }
        }

        return results;

    }

    private class State {
        private final StationRepository stationRepository;

        private LocalTime boardingTime;
        private Station boardingStation;
        private String routeCode;
        private String tripId;
        private String serviceId;
        private int passedStops;
        private int tripCost;

        private State(StationRepository stationRepository) {
            this.stationRepository = stationRepository;
            reset();
        }

        public void board(Relationship relationship) {
            boardingStation = stationRepository.getStation(relationship.getProperty(STATION_ID).toString()).get();
            routeCode = relationship.getProperty(ROUTE_ID).toString();
        }

        public RawVehicleStage getVehicleStage(Relationship relationship) {
            String stationId = relationship.getProperty(STATION_ID).toString();
            Station departStation = stationRepository.getStation(stationId).get();
            RawVehicleStage rawVehicleStage = new RawVehicleStage(boardingStation, routeCode,
                    TransportMode.Tram, routeIdToClass.map(routeCode));
            rawVehicleStage.setTripId(tripId);
            rawVehicleStage.setDepartTime(boardingTime);
            rawVehicleStage.setServiceId(serviceId);
            rawVehicleStage.setLastStation(departStation, passedStops);
//            LocalTime arrivalTime = currentTime.plusMinutes(currentCost);
//            int cost = TramTime.diffenceAsMinutes(TramTime.of(boardingTime), TramTime.of(arrivalTime));
            rawVehicleStage.setCost(tripCost);
            reset();
            return rawVehicleStage;
        }

        private void reset() {
            passedStops = -1; // don't count single stops
            tripId = "";
            routeCode = "";
            tripId = "";
            serviceId = "";
        }

        public void beginTrip(Relationship relationship) {
            tripCost = 0;
            tripId = relationship.getProperty(TRIP_ID).toString();
            boardingTime = (LocalTime) relationship.getProperty(TIME);
        }

        public void toService(Relationship relationship) {
            serviceId = relationship.getProperty(SERVICE_ID).toString();
        }

        public void passStop(Relationship relationship) {
            tripCost = tripCost + (int)relationship.getProperty(COST);
            passedStops = passedStops + 1;
        }
    }

    private class OLDState {
        private final RouteCodeToClassMapper routeIdToClass;
        private final StationRepository stationRepository;

        private boolean onTram = false;
        private LocalTime currentTime;
        private String tripId;
        private String serviceId;
        private String routeCode;
        private boolean isWalking;
        private int currentCost;
        private Location firstLocation;
        private LocalTime boardingTime;
        private int boardingPending = -1;

        public OLDState(RouteCodeToClassMapper routeIdToClass, StationRepository stationRepository, LocalTime queryTime) {
            this.routeIdToClass = routeIdToClass;
            this.stationRepository = stationRepository;
            this.currentTime = queryTime;
        }

        public void board(Node boardingNode) {
            boardingPending = currentCost;
            //boardingTime = currentTime.plusMinutes(currentCost);
            onTram = true;
            serviceId = boardingNode.getProperty(GraphStaticKeys.SERVICE_ID).toString();
            routeCode = boardingNode.getProperty(GraphStaticKeys.ROUTE_ID).toString();
        }

        public boolean isOnTram() {
            return onTram;
        }

        public RawVehicleStage departTram() {
            RawVehicleStage rawVehicleStage = new RawVehicleStage(firstLocation, routeCode,
                    TransportMode.Tram, routeIdToClass.map(routeCode));
            rawVehicleStage.setTripId(tripId);
            rawVehicleStage.setDepartTime(boardingTime);
            rawVehicleStage.setServiceId(serviceId);
            LocalTime arrivalTime = currentTime.plusMinutes(currentCost);
            int cost = TramTime.diffenceAsMinutes(TramTime.of(boardingTime), TramTime.of(arrivalTime));
            rawVehicleStage.setCost(cost);
            return rawVehicleStage;
        }

        public void currentTime(LocalTime currentTime) {
            if (boardingPending>0) {
                boardingTime = currentTime.minusMinutes(currentCost-boardingPending);
                boardingPending = -1;
            }
            this.currentTime = currentTime;
            this.currentCost = 0;
        }

        public void currentTrip(String tripId) {
            this.tripId = tripId;
        }

        public void beginWalk() {
            isWalking = true;
        }

        public boolean isWalking() {
            return isWalking;
        }

        public RawStage endWalk(Node node) {
            String stationId = node.getProperty(GraphStaticKeys.STATION_ID).toString();
            Location dest = stationRepository.getStation(stationId).get();
            return new RawWalkingStage(firstLocation, dest, currentCost);
        }

        public void recordCost(Iterable<Relationship> relationships) {
            Relationship relationship = relationships.iterator().next();
            recordCost(relationship);
        }

        public void recordCost(Relationship relationship) {
            int cost = (int) relationship.getProperty(COST);
            currentCost = currentCost + cost;
        }

        public void recordLocation(Node node) {
            if (!onTram) {
                String id = node.getProperty(STATION_ID).toString();
                firstLocation = stationRepository.getStation(id).get();
            }
        }
    }


}
