package com.tramchester.services;

import com.tramchester.domain.Station;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.StationIndexs;
import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexHits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SpatialService extends StationIndexs {
    public static final int NUMBER_OF_NEAREST = 6;
    public static final double DISTANCE_IN_KM = 30;
    private GraphDatabaseService graphDatabaseService;

    public SpatialService(GraphDatabaseService graphDatabaseService, RelationshipFactory relationshipFactory) {
        super(graphDatabaseService, relationshipFactory, false);
        this.graphDatabaseService = graphDatabaseService;
    }

    public List<Station> reorderNearestStations(Double latitude, Double longitude, Map<String, Station> stations) {
        Transaction tx = graphDatabaseService.beginTx();
        try {
            List<Station> sorted = stations.values().stream()
                    .sorted((s1, s2) -> s1.getName().compareTo(s2.getName())).collect(Collectors.toList());

            List<Node> nearestStations = getNearestStationsTo(latitude, longitude, NUMBER_OF_NEAREST);
            List<Station> reorderedStations = new ArrayList<>();

            for (Node node : nearestStations) {
                String id = node.getProperty(GraphStaticKeys.ID).toString();
                Station nearestStation = stations.get(id);
                if (nearestStation != null) {
                    nearestStation.setProximityGroup("Nearest Stops");
                    reorderedStations.add(nearestStation);
                    sorted.remove(nearestStation);
                }
            }

            for (Station station : sorted) {
                    station.setProximityGroup("All Stops");
                    reorderedStations.add(station);
            }
            tx.success();
            return reorderedStations;
        } finally {
            tx.close();
        }
    }

    public List<Node> getNearestStationsTo(double latitude, double longitude, int count) {
        Map<String, Object> params = new HashMap<>();
        params.put(LayerNodeIndex.POINT_PARAMETER, new Double[]{latitude, longitude});
        params.put(LayerNodeIndex.DISTANCE_IN_KM_PARAMETER, DISTANCE_IN_KM);

        IndexHits<Node> query = getSpatialIndex().query(LayerNodeIndex.WITHIN_DISTANCE_QUERY, params);

        return StreamSupport.stream(query.spliterator(),false).limit(count).collect(Collectors.toList());

    }

}
