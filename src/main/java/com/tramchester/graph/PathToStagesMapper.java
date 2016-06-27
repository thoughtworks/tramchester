package com.tramchester.graph;

import com.tramchester.domain.*;
import com.tramchester.graph.Nodes.*;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TransportRelationship;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class PathToStagesMapper {
    private static final Logger logger = LoggerFactory.getLogger(PathToStagesMapper.class);

    private NodeFactory nodeFactory;
    private RouteCodeToClassMapper routeIdToClass;
    private StationRepository stationRepository;

    public PathToStagesMapper(NodeFactory nodeFactory, RouteCodeToClassMapper routeIdToClass,
                              StationRepository stationRepository) {
        this.nodeFactory = nodeFactory;
        this.routeIdToClass = routeIdToClass;
        this.stationRepository = stationRepository;
    }

    public List<TransportStage> mapStages(WeightedPath path, int minsPastMidnight) {
        List<TransportStage> stages = new ArrayList<>();

        Iterable<Relationship> relationships = path.relationships();
        RelationshipFactory relationshipFactory = new RelationshipFactory();

        logger.info("Mapping path to stages, weight is " + path.weight());

        int totalCost = 0;
        RouteStationNode boardNode = null;
        RawVehicleStage currentStage = null;
        String firstStationId = null;
        int boardTime = -1;
        for (Relationship graphRelationship : relationships) {
            TransportRelationship transportRelationship = relationshipFactory.getRelationship(graphRelationship);

            TramNode firstNode = nodeFactory.getNode(graphRelationship.getStartNode());
            TramNode secondNode = nodeFactory.getNode(graphRelationship.getEndNode());
            String endNodeId = secondNode.getId();

            int cost = transportRelationship.getCost();
            totalCost += cost;

            // todo refactor out first and subsequent stage handling
            int elapsedTime = minsPastMidnight + totalCost;

            if (transportRelationship.isBoarding()) {
                // station -> route station
                boardNode = (RouteStationNode) secondNode;
                boardTime = elapsedTime;
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
                            transportRelationship.getMode(), tramRouteClass, boardTime);
                    currentStage.setServiceId(serviceId);
                }
            } else if (transportRelationship.isDepartTram()) {
                // route station -> station
                StationNode departNode = (StationNode) secondNode;
                String stationName = departNode.getName();
                logger.info(format("Depart tram: at:'%s' to: '%s' '%s' at %s", firstNode.getId(), stationName, endNodeId,
                        elapsedTime));
                currentStage.setLastStation(stationRepository.getStation(endNodeId));
                currentStage.setDepartTime(elapsedTime);
                stages.add(currentStage);
                logger.info(format("Added stage: '%s' at time %s",currentStage, elapsedTime));
                currentStage = null;
            } else if (transportRelationship.isWalk()) {

                Location begin;
                int walkStartTime;

                if (firstNode.isQuery()) {
                    QueryNode queryNode = (QueryNode) firstNode;
                    begin = new MyLocation(queryNode.getLatLon());
                    walkStartTime = minsPastMidnight;
                } else {
                    begin = stationRepository.getStation(firstNode.getId());
                    walkStartTime = elapsedTime;
                }

                StationNode dest = (StationNode) secondNode;
                Location destStation = stationRepository.getStation(dest.getId());
                WalkingStage walkingStage = new WalkingStage(begin, destStation, walkStartTime, cost);
                logger.info("Adding walk " + walkingStage);
                stages.add(walkingStage);

            }
        }
        logger.info(format("Number of stages: %s Total cost:%s Finish: %s",stages.size(), totalCost,
                totalCost+minsPastMidnight));
        return stages;
    }

}
