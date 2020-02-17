package com.tramchester.services;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.StationIndexs;
import com.tramchester.repository.StationRepository;
import org.neo4j.gis.spatial.pipes.GeoPipeFlow;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class SpatialService {
    private static final Logger logger = LoggerFactory.getLogger(SpatialService.class);

    private final GraphDatabaseService graphDatabaseService;
    private final StationRepository stationRepository;
    private final TramchesterConfig config;
    private final GraphQuery graphQuery;

    public SpatialService(GraphDatabaseService graphDatabaseService, StationRepository stationRepository,
                          TramchesterConfig config, GraphQuery graphQuery) {
        this.graphDatabaseService = graphDatabaseService;
        this.stationRepository = stationRepository;
        this.config = config;
        this.graphQuery = graphQuery;
    }


    public List<Station> getNearestStations(LatLong latLong) {
        List<Station> results = new LinkedList<>();
        List<String> ids = getNearestStationsTo(latLong, config.getNumOfNearestStops());
        ids.forEach(id -> {
            stationRepository.getStation(id).ifPresent(results::add);
        });
        return results;
    }

    public List<StationDTO> reorderNearestStations(LatLong latLong, List<Station> sortedStations) {
        Transaction tx = graphDatabaseService.beginTx();
        List<Station> seen = new LinkedList<>();
        try {
            List<String> nearestStations = getNearestStationsToNoTransaction(latLong, config.getNumOfNearestStops());
            List<StationDTO> reorderedStations = new ArrayList<>();

            if (nearestStations.size()==0) {
                logger.warn("Unable to find stations close to " + latLong);
            }

            for (String id : nearestStations) {
                Optional<Station> maybeNearestStation = stationRepository.getStation(id);
                maybeNearestStation.ifPresent(nearestStation -> {
                    StationDTO displayStation = new StationDTO(nearestStation, ProximityGroup.NEAREST_STOPS);
                    reorderedStations.add(displayStation);
                    seen.add(nearestStation);
                });
            }

            List<StationDTO> remainingStations = sortedStations.stream().filter(station -> !seen.contains(station)).
                    map(station -> new StationDTO(station, ProximityGroup.ALL)).
                    collect(Collectors.toList());

            reorderedStations.addAll(remainingStations);
            tx.success();
            return reorderedStations;
        } finally {
            tx.close();
        }
    }

    public List<String> getNearestStationsTo(LatLong latLong, int numberOfNearest) {
        Transaction tx = graphDatabaseService.beginTx();
        List<String> result;
        try {
            result = getNearestStationsToNoTransaction(latLong, numberOfNearest);
            tx.success();
        }
        finally {
            tx.close();
        }
        logger.info(format("Found %s stations close to %s", result.size(), latLong));
        return result;
    }

    // TODO Range depends on location, i.e. in city center limit the range, further out have a larger range?
    private List<String> getNearestStationsToNoTransaction(LatLong latLong, int count) {
        List<GeoPipeFlow> results = graphQuery.getSpatialLayer().findClosestPointsTo(LatLong.getCoordinate(latLong),
                config.getNearestStopRangeKM());

        List<String> ids =results.stream().limit(count).
                map(item -> (String)item.getRecord().getGeomNode().getProperty(GraphStaticKeys.ID)).
                collect(Collectors.toList());

        return ids;
    }

}
