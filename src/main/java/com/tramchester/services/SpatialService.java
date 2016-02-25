package com.tramchester.services;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Location;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.DisplayStation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.StationIndexs;
import com.tramchester.repository.StationRepository;
import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexHits;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SpatialService extends StationIndexs {
    public static final String ALL_STOPS_PROX_GROUP = "All Stops";
    // TODO these both into config
    //public static final int NUMBER_OF_NEAREST = 6;
    //public static final double DISTANCE_IN_KM = 30;

    private GraphDatabaseService graphDatabaseService;
    private StationRepository stationRepository;
    private TramchesterConfig config;

    public SpatialService(GraphDatabaseService graphDatabaseService, RelationshipFactory relationshipFactory,
                          StationRepository stationRepository,
                          TramchesterConfig config) {
        super(graphDatabaseService, relationshipFactory, false);
        this.graphDatabaseService = graphDatabaseService;
        this.stationRepository = stationRepository;
        this.config = config;
    }

    public List<DisplayStation> reorderNearestStations(LatLong latLong, List<Station> sortedStations) {
        Transaction tx = graphDatabaseService.beginTx();
        List<Station> seen = new LinkedList<>();
        try {
            List<String> nearestStations = getNearestStationsToNoTransaction(latLong, config.getNumOfNearestStops());
            List<DisplayStation> reorderedStations = new ArrayList<>();

            for (String id : nearestStations) {
                Station nearestStation = stationRepository.getStation(id);
                if (nearestStation != null) {
                    DisplayStation displayStation = new DisplayStation(nearestStation, "Nearest Stops");
                    reorderedStations.add(displayStation);
                    seen.add(nearestStation);
                }
            }

            reorderedStations.addAll(sortedStations.stream().filter(station -> !seen.contains(station)).
                    map(station -> new DisplayStation(station, ALL_STOPS_PROX_GROUP)).
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
        Map<String, Object> params = new HashMap<>();
        params.put(LayerNodeIndex.POINT_PARAMETER, new Double[]{latLong.getLat(), latLong.getLon()});
        params.put(LayerNodeIndex.DISTANCE_IN_KM_PARAMETER, config.getNearestStopRangeKM());

        IndexHits<Node> query = getSpatialIndex().query(LayerNodeIndex.WITHIN_DISTANCE_QUERY, params);

        List<String> ids = StreamSupport.stream(query.spliterator(), false).limit(count)
                .map(node-> (String)node.getProperty(GraphStaticKeys.ID)).collect(Collectors.toList());

        return ids;
    }
}
