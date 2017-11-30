package com.tramchester.graph;

import com.tramchester.domain.*;
import com.tramchester.graph.Nodes.QueryNode;
import com.tramchester.graph.Nodes.RouteStationNode;
import com.tramchester.graph.Nodes.StationNode;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class MapTransportRelationshipsToStages {
    private static final Logger logger = LoggerFactory.getLogger(MapTransportRelationshipsToStages.class);

    private RouteCodeToClassMapper routeIdToClass;
    private StationRepository stationRepository;

    public MapTransportRelationshipsToStages(RouteCodeToClassMapper routeIdToClass,
                                             StationRepository stationRepository) {
        this.routeIdToClass = routeIdToClass;
        this.stationRepository = stationRepository;
    }

    public List<RawStage> mapStages(List<TransportRelationship> transportRelationships, int minsPastMidnight) {
        int totalCost = 0;
        MappingState state = new MappingState();

        for (TransportRelationship transportRelationship : transportRelationships) {
            TramNode firstNode = transportRelationship.getStartNode();
            TramNode secondNode = transportRelationship.getEndNode();

            String endNodeId = secondNode.getId();

            int cost = transportRelationship.getCost();
            totalCost += cost;

            int elapsedTime = minsPastMidnight + totalCost;

            String firstNodeId = firstNode.getId();
            if (transportRelationship.isBoarding()) {
                logger.info(format("Board tram: at:'%s' from '%s' at %s", secondNode, firstNode, elapsedTime));
                recordBoarding(totalCost, state, firstNode, (RouteStationNode) secondNode, firstNodeId);
            } else if (transportRelationship.isEnterPlatform()) {
                recordEnterPlatform(state, firstNode, secondNode, elapsedTime, firstNodeId);
            } else if (transportRelationship.isGoesTo()) {
                recordGoesTo(state, transportRelationship, elapsedTime);
            } else if (transportRelationship.isDepartTram()) {
                recordDepart(totalCost, state, secondNode, endNodeId, elapsedTime, firstNodeId);
            } else if (transportRelationship.isLeavePlatform()) {
                logger.info(format("Depart platform %s %s", endNodeId, secondNode.getName()));
            } else if (transportRelationship.isWalk()) {
                recordWalk(state, firstNode, (StationNode) secondNode, cost, firstNodeId);
            }
        }
        List<RawStage> stages = state.getStages();
        logger.info(format("Number of stages: %s Total cost:%s Finish: %s", stages.size(), totalCost, totalCost + minsPastMidnight));
        return stages;
    }

    private void recordWalk(MappingState state, TramNode firstNode, StationNode secondNode, int cost, String firstNodeId) {
        Location begin;
        if (firstNode.isQuery()) {
            QueryNode queryNode = (QueryNode) firstNode;
            begin = new MyLocation(queryNode.getLatLon());
        } else {
            begin = stationRepository.getStation(firstNodeId).get();
        }
        StationNode dest = secondNode;
        state.addWalkingStage(begin, dest, cost);
    }

    private void recordDepart(int totalCost, MappingState state, TramNode secondNode, String endNodeId, int elapsedTime, String firstNodeId) {
        // route station  -> station
        String stationName = secondNode.getName();
        logger.info(format("Depart tram: at:'%s' to: '%s' '%s' at %s", firstNodeId, stationName, endNodeId, elapsedTime));
        // are we leaving to a station or to a platform?
        String stageId = endNodeId;
        if (secondNode.isPlatform()) {
            stageId = Station.formId(endNodeId);
        }
        state.departService(totalCost, stageId, elapsedTime);
    }

    private void recordGoesTo(MappingState state, TransportRelationship transportRelationship, int elapsedTime) {
        // routeStation -> routeStation
        GoesToRelationship goesToRelationship = (GoesToRelationship) transportRelationship;
        String serviceId = goesToRelationship.getService();
        logger.info(format("Add stage goes to %s, service %s, elapsed %s", goesToRelationship.getDest(), serviceId, elapsedTime));

        if (!state.isOnService()) {
            state.boardService(transportRelationship, serviceId);
        }
    }

    private void recordEnterPlatform(MappingState state, TramNode firstNode, TramNode secondNode, int elapsedTime, String firstNodeId) {
        logger.info(format("Cross to platfrom '%s' from '%s' at %s", secondNode, firstNode, elapsedTime));
        if (!state.hasFirstStation()) {
            state.setFirstStation(firstNodeId);
        }
    }

    private void recordBoarding(int totalCost, MappingState state, TramNode firstNode, RouteStationNode secondNode, String firstNodeId) {
        // platform|station -> route station
        state.setBoardNode(secondNode);
        if (!state.hasFirstStation()) {
            // no platform seen
            state.setFirstStation(firstNodeId);
            if (firstNode.isPlatform()) {
                // boarding from platform
                state.setFirstStation(Station.formId(firstNodeId));
            }
        }
        state.setServiceStart(totalCost);
        if (state.isOnService()) {
            logger.error(format("Encountered boarding (at %s) before having departed an existing stage %s",
                    state.getBoardNode(), state.getCurrentStage()));
        }
    }

    private class MappingState {
        List<RawStage> stages;
        private int serviceStart;
        private RouteStationNode boardNode;
        private RawVehicleStage currentStage;
        private String firstStationId;

        public MappingState() {
            serviceStart = 0;
            boardNode = null;
            currentStage = null;
            firstStationId = "";
            stages = new ArrayList<>();
        }

        public void setBoardNode(RouteStationNode boardNode) {
            this.boardNode = boardNode;
        }

        public boolean hasFirstStation() {
            return !firstStationId.isEmpty();
        }

        public void setFirstStation(String id) {
            firstStationId = id;
        }

        public void setServiceStart(int serviceStart) {
            this.serviceStart = serviceStart;
        }

        public boolean isOnService() {
            return currentStage!=null;
        }

        public RouteStationNode getBoardNode() {
            return boardNode;
        }

        public RawVehicleStage getCurrentStage() {
            return currentStage;
        }

        public void boardService(TransportRelationship transportRelationship, String serviceId) {
            String routeName = boardNode.getRouteName();
            String routeId = boardNode.getRouteId();
            String tramRouteClass = routeIdToClass.map(routeId);
            Station firstStation = stationRepository.getStation(firstStationId).get();
            currentStage = new RawVehicleStage(firstStation, routeName,
                    transportRelationship.getMode(), tramRouteClass);
            currentStage.setServiceId(serviceId);
        }

        public void departService(int totalCost, String stationId, int elapsedTime) {
            Station lastStation = stationRepository.getStation(stationId).get();
            currentStage.setLastStation(lastStation);
            currentStage.setCost(totalCost - serviceStart);
            logger.info(format("Added stage: '%s' at time %s", currentStage, elapsedTime));
            stages.add(currentStage);
            doReset();
        }

        private void doReset() {
            serviceStart = 0;
            currentStage = null;
            firstStationId = "";
        }

        public List<RawStage> getStages() {
            return stages;
        }

        public void addWalkingStage(Location begin, StationNode dest, int cost) {
            Location destStation = stationRepository.getStation(dest.getId()).get();
            RawWalkingStage walkingStage = new RawWalkingStage(begin, destStation, cost);
            logger.info("Adding walk " + walkingStage);
            stages.add(walkingStage);
        }
    }

}
