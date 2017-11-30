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
        int serviceStart = 0;
        RouteStationNode boardNode = null;
        RawVehicleStage currentStage = null;
        String firstStationId = "";

        List<RawStage> stages = new ArrayList<>();

        for (TransportRelationship transportRelationship : transportRelationships) {
            TramNode firstNode = transportRelationship.getStartNode();
            TramNode secondNode = transportRelationship.getEndNode();

            String endNodeId = secondNode.getId();

            int cost = transportRelationship.getCost();
            totalCost += cost;

            // todo refactor out first and subsequent stage handling
            int elapsedTime = minsPastMidnight + totalCost;

            if (transportRelationship.isBoarding()) {
                // platform|station -> route station
                boardNode = (RouteStationNode) secondNode;
                if (firstStationId.isEmpty()) {
                    // no platform
                    firstStationId = firstNode.getId();
                    if (firstNode.isPlatform()) {
                        // boarding from platform
                        firstStationId = Station.formId(firstStationId);
                    }
                }
                serviceStart = totalCost;
                logger.info(format("Board tram: at:'%s' from '%s' at %s", secondNode, firstNode, elapsedTime));
                if (currentStage!=null) {
                    logger.error(format("Encountered boarding (at %s) before having departed an existing stage %s",
                            boardNode, currentStage));
                }
            } else if (transportRelationship.isEnterPlatform()) {
                // to do capture platform in stages
                logger.info(format("Cross to platfrom '%s' from '%s' at %s", secondNode, firstNode, elapsedTime));
                if (firstStationId.isEmpty()) {
                    firstStationId = firstNode.getId();
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
                    Station firstStation = stationRepository.getStation(firstStationId).get();
                    currentStage = new RawVehicleStage(firstStation, routeName,
                            transportRelationship.getMode(), tramRouteClass);
                    currentStage.setServiceId(serviceId);
                }
            } else if (transportRelationship.isDepartTram()) {
                // route station  -> station
                // only if not left tram to platform, i.e. not tram journey
                String stationName = secondNode.getName();
                logger.info(format("Depart tram: at:'%s' to: '%s' '%s' at %s", firstNode.getId(), stationName, endNodeId,
                        elapsedTime));
                String stationId = endNodeId;
                if (secondNode.isPlatform()) {
                    stationId = Station.formId(endNodeId);
                }
                Station lastStation = stationRepository.getStation(stationId).get();
                currentStage.setLastStation(lastStation);
                currentStage.setCost(totalCost - serviceStart);
                serviceStart = 0;
                stages.add(currentStage);
                logger.info(format("Added stage: '%s' at time %s", currentStage, elapsedTime));
                currentStage = null;
                firstStationId = "";
            } else if (transportRelationship.isLeavePlatform()) {
                logger.info(format("Depart platform %s %s",endNodeId, secondNode.getName()));
            } else if (transportRelationship.isWalk()) {
                Location begin;
                if (firstNode.isQuery()) {
                    QueryNode queryNode = (QueryNode) firstNode;
                    begin = new MyLocation(queryNode.getLatLon());
                } else {
                    begin = stationRepository.getStation(firstNode.getId()).get();
                }

                StationNode dest = (StationNode) secondNode;
                Location destStation = stationRepository.getStation(dest.getId()).get();
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
