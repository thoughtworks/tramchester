package com.tramchester.graph;

import com.tramchester.domain.*;
import com.tramchester.domain.Platform;
import com.tramchester.graph.Nodes.BoardPointNode;
import com.tramchester.graph.Nodes.StationNode;
import com.tramchester.graph.Relationships.TransportRelationship;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class MappingState {
    private static final Logger logger = LoggerFactory.getLogger(MappingState.class);

    private RouteCodeToClassMapper routeIdToClass;
    private StationRepository stationRepository;
    private PlatformRepository platformRepository;
    private final LocalTime queryTime;

    private List<RawStage> stages;
    private int serviceStart;
    private BoardPointNode boardingNode;
    private RawVehicleStage currentStage;
    private String firstStationId;
    private int totalCost;
    private Platform boardingPlatform;

    public MappingState(PlatformRepository platformRepository, StationRepository stationRepository,
                        LocalTime queryTime, RouteCodeToClassMapper routeIdToClass) {
        this.platformRepository = platformRepository;
        this.queryTime = queryTime;
        this.routeIdToClass = routeIdToClass;
        this.stationRepository = stationRepository;
        //
        serviceStart = 0;
        totalCost = 0;
        boardingNode = null;
        currentStage = null;
        firstStationId = "";
        boardingPlatform = null;
        stages = new ArrayList<>();
    }

    public void setBoardingNode(BoardPointNode boardingNode) {
        this.boardingNode = boardingNode;
    }

    public boolean hasFirstStation() {
        return !firstStationId.isEmpty();
    }

    public void setFirstStation(String id) {
        firstStationId = id;
    }

    public boolean isOnService() {
        return currentStage!=null;
    }

    public BoardPointNode getBoardingNode() {
        return boardingNode;
    }

    public RawVehicleStage getCurrentStage() {
        return currentStage;
    }

    public void recordServiceStart() {
        this.serviceStart = totalCost;
    }

    public void boardService(TransportRelationship transportRelationship, String serviceId) {
        String routeName = boardingNode.getRouteName();
        String routeId = boardingNode.getRouteId();
        String tramRouteClass = routeIdToClass.map(routeId);
        Station firstStation = stationRepository.getStation(firstStationId).get();
        currentStage = new RawVehicleStage(firstStation, routeName,
                transportRelationship.getMode(), tramRouteClass);
        currentStage.setServiceId(serviceId);
        if (boardingPlatform !=null) {
            currentStage.setPlatform(boardingPlatform);
        }
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
        boardingPlatform = null;
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

    public LocalTime getElapsedTime() {
        return queryTime.plusMinutes(totalCost);
    }

    public int getTotalCost() {
        return totalCost;
    }

    public List<RawStage> getStages() {
        return stages;
    }

    public void setPlatform(String platformId) {
        Optional<Platform> result = platformRepository.getPlatformById(platformId);
        if (result.isPresent()) {
            boardingPlatform = result.get();
        }
    }
}
