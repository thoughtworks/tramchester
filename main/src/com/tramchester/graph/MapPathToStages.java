package com.tramchester.graph;


import com.tramchester.domain.*;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.PlatformRepository;
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
import java.util.Optional;

import static com.tramchester.graph.GraphStaticKeys.*;
import static com.tramchester.graph.GraphStaticKeys.RouteStation.ROUTE_NAME;
import static java.lang.String.format;


public class MapPathToStages {
    private static final Logger logger = LoggerFactory.getLogger(MapPathToStages.class);

    private final RouteCodeToClassMapper routeIdToClass;
    private final StationRepository stationRepository;
    private final MyLocationFactory myLocationFactory;
    private PlatformRepository platformRepository;

    public MapPathToStages(RouteCodeToClassMapper routeIdToClass, StationRepository stationRepository,
                           MyLocationFactory myLocationFactory, PlatformRepository platformRepository) {
        this.routeIdToClass = routeIdToClass;
        this.stationRepository = stationRepository;
        this.myLocationFactory = myLocationFactory;
        this.platformRepository = platformRepository;
    }

    // TODO Use Traversal State from the path instead of the Path itself
    public List<RawStage> mapDirect(WeightedPath path) {
        ArrayList<RawStage> results = new ArrayList<>();
        State state = new State(stationRepository, myLocationFactory, platformRepository);

        for(Relationship relationship : path.relationships()) {
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
                    state.beginTrip(relationship);
                    break;
                case TRAM_GOES_TO:
                    state.passStop(relationship);
                    break;
                case WALKS_TO:
                    results.add(state.walk(relationship));
                    break;
                case TO_SERVICE:
                    state.toService(relationship);
                    break;
                case ENTER_PLATFORM:
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

    private class State {
        private final StationRepository stationRepository;
        private final MyLocationFactory myLocationFactory;
        private final PlatformRepository platformRepository;

        private TramTime boardingTime;
        private Station boardingStation;
        private String routeCode;
        private String tripId;
        private String serviceId;
        private int passedStops;
        private int tripCost;
        private Optional<Platform> boardingPlatform;
        private String routeName;

        private State(StationRepository stationRepository, MyLocationFactory myLocationFactory, PlatformRepository platformRepository) {
            this.stationRepository = stationRepository;
            this.myLocationFactory = myLocationFactory;
            this.platformRepository = platformRepository;
            reset();
        }

        public void board(Relationship relationship) {
            boardingStation = stationRepository.getStation(relationship.getProperty(STATION_ID).toString()).get();
            routeCode = relationship.getProperty(ROUTE_ID).toString();
            routeName = relationship.getProperty(ROUTE_NAME).toString();
            String stopId = relationship.getProperty(PLATFORM_ID).toString();
            boardingPlatform = platformRepository.getPlatformById(stopId);
        }

        public RawVehicleStage depart(Relationship relationship) {
            String stationId = relationship.getProperty(STATION_ID).toString();
            Station departStation = stationRepository.getStation(stationId).get();
            RawVehicleStage rawVehicleStage = new RawVehicleStage(boardingStation, routeName,
                    TransportMode.Tram, routeIdToClass.map(routeCode));
            rawVehicleStage.setTripId(tripId);
            rawVehicleStage.setDepartTime(boardingTime);
            rawVehicleStage.setServiceId(serviceId);
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
            serviceId = "";
            tripCost = 0;
            boardingPlatform = Optional.empty();
        }

        public void beginTrip(Relationship relationship) {
            String newTripId = relationship.getProperty(TRIP_ID).toString();

            if (tripId.isEmpty()) {
                this.tripId = newTripId;
                LocalTime property = (LocalTime) relationship.getProperty(TIME);
                boardingTime = TramTime.of(property);
            } else if (!tripId.equals(newTripId)){
                throw new RuntimeException(format("Mid flight change of trip from %s to %s", tripId, newTripId));
            }
        }

        public void toService(Relationship relationship) {
            serviceId = relationship.getProperty(SERVICE_ID).toString();
        }

        public void passStop(Relationship relationship) {
            tripCost = tripCost + getCost(relationship);
            passedStops = passedStops + 1;
        }

        public RawWalkingStage walk(Relationship relationship) {
            int cost = getCost(relationship);
            String stationId = relationship.getProperty(STATION_ID).toString();
            Location destination = stationRepository.getStation(stationId).get();
            Node startNode = relationship.getStartNode();

            double lat = (double)startNode.getProperty(GraphStaticKeys.Station.LAT);
            double lon =  (double)startNode.getProperty(GraphStaticKeys.Station.LONG);
            Location start = myLocationFactory.create(new LatLong(lat,lon));

            RawWalkingStage rawWalkingStage = new RawWalkingStage(start, destination, cost);
            return rawWalkingStage;
        }

        private int getCost(Relationship relationship) {
            return (int)relationship.getProperty(COST);
        }

    }

}
