package com.tramchester.services;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.integration.graph.GraphStaticKeys;
import com.tramchester.integration.graph.Relationships.RelationshipFactory;
import com.tramchester.integration.graph.StationIndexs;
import com.tramchester.integration.repository.StationRepository;
import org.neo4j.gis.spatial.SpatialDatabaseService;
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

public class SpatialService extends StationIndexs {
    private static final Logger logger = LoggerFactory.getLogger(SpatialService.class);

    private GraphDatabaseService graphDatabaseService;
    private StationRepository stationRepository;
    private TramchesterConfig config;

    public SpatialService(GraphDatabaseService graphDatabaseService, RelationshipFactory relationshipFactory,
                          StationRepository stationRepository,
                          SpatialDatabaseService spatialDatabaseService, TramchesterConfig config) {
        super(graphDatabaseService, relationshipFactory, spatialDatabaseService, false);
        this.graphDatabaseService = graphDatabaseService;
        this.stationRepository = stationRepository;
        this.config = config;
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

            reorderedStations.addAll(sortedStations.stream().filter(station -> !seen.contains(station)).
                    map(station -> new StationDTO(station, ProximityGroup.ALL)).
                    collect(Collectors.toList()));
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
        return result;
    }

    private List<String> getNearestStationsToNoTransaction(LatLong latLong, int count) {
        List<GeoPipeFlow> results = getSpatialLayer().findClosestPointsTo(LatLong.getCoordinate(latLong),
                config.getNearestStopRangeKM());

        List<String> ids =results.stream().limit(count).map(item -> (String)item.getRecord().getGeomNode().getProperty(GraphStaticKeys.ID)).
                collect(Collectors.toList());

        return ids;
    }
}
