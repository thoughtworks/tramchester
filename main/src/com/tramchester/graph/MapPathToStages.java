package com.tramchester.graph;


import com.tramchester.domain.*;
import com.tramchester.graph.Relationships.PathToTransportRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.tramchester.graph.GraphStaticKeys.COST;
import static com.tramchester.graph.GraphStaticKeys.STATION_ID;


public class MapPathToStages {
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

    // TODO Node Operations
    public List<RawStage> mapDirect(WeightedPath path, LocalTime queryTime) {
        ArrayList<RawStage> results = new ArrayList<>();
        State state = new State(routeIdToClass, stationRepository, queryTime);

        for(Node node : path.nodes()) {
            if (node.hasRelationship(Direction.INCOMING)) {
                state.recordCost(node.getRelationships(Direction.INCOMING));
            }
            TransportGraphBuilder.Labels nodeLabel =
                    TransportGraphBuilder.Labels.valueOf(node.getLabels().iterator().next().name());
            switch (nodeLabel) {
                case MINUTE:
                    state.currentTime((LocalTime) node.getProperty(GraphStaticKeys.TIME));
                    state.currentTrip(node.getProperty(GraphStaticKeys.TRIP_ID).toString());
                    break;
                case HOUR:
                    break;
                case ROUTE_STATION:
                    state.recordLocation(node);
                    break;
                case SERVICE:
                    if (!state.isOnTram()) {
                        state.board(node);
                    }
                    break;
                case PLATFORM:
                    if (state.isOnTram()) {
                        results.add(state.departTram());
                    }
                    break;
                case QUERY_NODE:
                    state.beginWalk();
                    break;
                case STATION:
                    if (state.isWalking()) {
                        results.add(state.endWalk(node));
                    }
                    break;
            }
        }
        return results;

    }

    private class State {
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

        public State(RouteCodeToClassMapper routeIdToClass, StationRepository stationRepository, LocalTime queryTime) {
            this.routeIdToClass = routeIdToClass;
            this.stationRepository = stationRepository;
            this.currentTime = queryTime;
        }

        public void board(Node node) {
            boardingPending = currentCost;
            //boardingTime = currentTime.plusMinutes(currentCost);
            onTram = true;
            serviceId = node.getProperty(GraphStaticKeys.SERVICE_ID).toString();
            routeCode = node.getProperty(GraphStaticKeys.ROUTE_ID).toString();
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
            int cost = (int) relationships.iterator().next().getProperty(COST);
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
