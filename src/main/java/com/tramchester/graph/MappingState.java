package com.tramchester.graph;

import com.tramchester.domain.*;
import com.tramchester.graph.Nodes.RouteStationNode;
import com.tramchester.graph.Nodes.StationNode;
import com.tramchester.graph.Relationships.TransportRelationship;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class MappingState {
    private static final Logger logger = LoggerFactory.getLogger(MappingState.class);

    private RouteCodeToClassMapper routeIdToClass;
    private StationRepository stationRepository;
    private final int minsPastMidnight;

    private List<RawStage> stages;
    private int serviceStart;
    private RouteStationNode boardNode;
    private RawVehicleStage currentStage;
    private String firstStationId;
    private int totalCost;

    public MappingState(int minsPastMidnight, RouteCodeToClassMapper routeIdToClass, StationRepository stationRepository) {
        this.minsPastMidnight = minsPastMidnight;
        this.routeIdToClass = routeIdToClass;
        this.stationRepository = stationRepository;
        serviceStart = 0;
        boardNode = null;
        currentStage = null;
        firstStationId = "";
        totalCost = 0;
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

    public void recordServiceStart() {
        this.serviceStart = totalCost;
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

    public void departService(String stationId) {
        Station lastStation = stationRepository.getStation(stationId).get();
        currentStage.setLastStation(lastStation);
        currentStage.setCost(totalCost - serviceStart);
        logger.info(format("Added stage: '%s' at time %s", currentStage, getElapsedTime()));
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

    public void addWalkingStage(Location begin, StationNode dest, int walkCost) {
        Location destStation = stationRepository.getStation(dest.getId()).get();
        RawWalkingStage walkingStage = new RawWalkingStage(begin, destStation, walkCost);
        logger.info("Adding walk " + walkingStage);
        stages.add(walkingStage);
    }

    public void incrementCost(int cost) {
        totalCost += cost;

    }

    public int getElapsedTime() {
        return minsPastMidnight + totalCost;
    }

    public int getTotalCost() {
        return totalCost;
    }
}
