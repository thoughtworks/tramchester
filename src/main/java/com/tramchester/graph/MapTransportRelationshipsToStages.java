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
        MappingState state = new MappingState(minsPastMidnight, routeIdToClass, stationRepository);

        for (TransportRelationship transportRelationship : transportRelationships) {
            TramNode firstNode = transportRelationship.getStartNode();
            TramNode secondNode = transportRelationship.getEndNode();

            String endNodeId = secondNode.getId();
            String firstNodeId = firstNode.getId();

            int currentStepCost = transportRelationship.getCost();
            state.incrementCost(currentStepCost);

            if (transportRelationship.isBoarding()) {
                recordBoarding(state, firstNode, (RouteStationNode) secondNode, firstNodeId);
            } else if (transportRelationship.isEnterPlatform()) {
                recordEnterPlatform(state, firstNode, secondNode, firstNodeId);
            } else if (transportRelationship.isGoesTo()) {
                recordGoesTo(state, transportRelationship);
            } else if (transportRelationship.isDepartTram()) {
                recordDepart(state, secondNode, endNodeId, firstNodeId);
            } else if (transportRelationship.isLeavePlatform()) {
                logger.info(format("Depart platform %s %s", endNodeId, secondNode.getName()));
            } else if (transportRelationship.isWalk()) {
                recordWalk(state, firstNode, (StationNode) secondNode, currentStepCost, firstNodeId);
            }
        }
        List<RawStage> stages = state.getStages();
        logger.info(format("Number of stages: %s Total cost:%s Finish: %s", stages.size(), state.getTotalCost(), state.getElapsedTime()));
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

    private void recordDepart(MappingState state, TramNode secondNode, String endNodeId, String firstNodeId) {
        // route station  -> station
        String stationName = secondNode.getName();
        logger.info(format("Depart tram: at:'%s' to: '%s' '%s' at %s", firstNodeId, stationName, endNodeId, state.getElapsedTime()));
        // are we leaving to a station or to a platform?
        String stageId = endNodeId;
        if (secondNode.isPlatform()) {
            stageId = Station.formId(endNodeId);
        }
        state.departService(stageId);
    }

    private void recordGoesTo(MappingState state, TransportRelationship transportRelationship) {
        // routeStation -> routeStation
        GoesToRelationship goesToRelationship = (GoesToRelationship) transportRelationship;
        String serviceId = goesToRelationship.getService();
        logger.info(format("Add stage goes to %s, service %s, elapsed %s", goesToRelationship.getDest(), serviceId, state.getElapsedTime()));

        if (!state.isOnService()) {
            state.boardService(transportRelationship, serviceId);
        }
    }

    private void recordEnterPlatform(MappingState state, TramNode firstNode, TramNode secondNode, String firstNodeId) {
        logger.info(format("Cross to platfrom '%s' from '%s' at %s", secondNode, firstNode, state.getElapsedTime()));
        if (!state.hasFirstStation()) {
            state.setFirstStation(firstNodeId);
        }
    }

    private void recordBoarding(MappingState state, TramNode firstNode, RouteStationNode secondNode, String firstNodeId) {
        logger.info(format("Board tram: at:'%s' from '%s' at %s", secondNode, firstNode, state.getElapsedTime()));
        // platform|station -> route station
        state.setBoardNode(secondNode);
        if (!state.hasFirstStation()) {
            state.setFirstStation(firstNodeId);
            if (firstNode.isPlatform()) {
                // boarding from platform
                state.setFirstStation(Station.formId(firstNodeId));
            }
        }
        state.recordServiceStart();
        if (state.isOnService()) {
            logger.error(format("Encountered boarding (at %s) before having departed an existing stage %s",
                    state.getBoardNode(), state.getCurrentStage()));
        }
    }

}
