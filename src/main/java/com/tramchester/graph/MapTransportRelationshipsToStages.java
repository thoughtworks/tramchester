package com.tramchester.graph;

import com.tramchester.domain.*;
import com.tramchester.graph.Nodes.*;
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

    public List<TransportStage> mapStages(List<TransportRelationship> transportRelationships, int minsPastMidnight) {
        int totalCost = 0;
        RouteStationNode boardNode = null;
        RawVehicleStage currentStage = null;
        String firstStationId = null;

        List<TransportStage> stages = new ArrayList<>();

        for (TransportRelationship transportRelationship : transportRelationships) {
            TramNode firstNode = transportRelationship.getStartNode();
            TramNode secondNode = transportRelationship.getEndNode();

            String endNodeId = secondNode.getId();

            int cost = transportRelationship.getCost();
            totalCost += cost;

            // todo refactor out first and subsequent stage handling
            int elapsedTime = minsPastMidnight + totalCost;

            if (transportRelationship.isBoarding()) {
                // station -> route station
                boardNode = (RouteStationNode) secondNode;
                firstStationId = firstNode.getId();
                logger.info(format("Board tram: at:'%s' from '%s' at %s", secondNode, firstNode, elapsedTime));
                if (currentStage!=null) {
                    logger.error(format("Encountered boarding (at %s) before having departed an existing stage %s",
                            boardNode, currentStage));
                }
            } else if (transportRelationship.isGoesTo()) {
                // routeStation -> routeStation
                GoesToRelationship goesToRelationship = (GoesToRelationship) transportRelationship;
                String serviceId = goesToRelationship.getService();
                logger.info(format("Add stage goes to %s, service %s, elapsed %s", goesToRelationship.getDest(),
                        serviceId, elapsedTime));

                if (currentStage==null) {
                    String routeName = boardNode.getRouteName();
                    String routeId = boardNode.getRouteId();
                    String tramRouteClass = routeIdToClass.map(routeId);
                    currentStage = new RawVehicleStage(stationRepository.getStation(firstStationId), routeName,
                            transportRelationship.getMode(), tramRouteClass);
                    currentStage.setServiceId(serviceId);
                }
            } else if (transportRelationship.isDepartTram()) {
                // route station -> station
                StationNode departNode = (StationNode) secondNode;
                String stationName = departNode.getName();
                logger.info(format("Depart tram: at:'%s' to: '%s' '%s' at %s", firstNode.getId(), stationName, endNodeId,
                        elapsedTime));
                currentStage.setLastStation(stationRepository.getStation(endNodeId));
                stages.add(currentStage);
                logger.info(format("Added stage: '%s' at time %s",currentStage, elapsedTime));
                currentStage = null;
            } else if (transportRelationship.isWalk()) {

                Location begin;
                if (firstNode.isQuery()) {
                    QueryNode queryNode = (QueryNode) firstNode;
                    begin = new MyLocation(queryNode.getLatLon());
                } else {
                    begin = stationRepository.getStation(firstNode.getId());
                }

                StationNode dest = (StationNode) secondNode;
                Location destStation = stationRepository.getStation(dest.getId());
                RawWalkingStage walkingStage = new RawWalkingStage(begin, destStation, cost);
                logger.info("Adding walk " + walkingStage);
                stages.add(walkingStage);
            }
        }
        logger.info(format("Number of stages: %s Total cost:%s Finish: %s",stages.size(), totalCost,
                totalCost+minsPastMidnight));
        return stages;
    }

}
