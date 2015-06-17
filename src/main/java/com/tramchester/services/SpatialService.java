package com.tramchester.services;

import com.tramchester.domain.Station;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.neo4j.gis.spatial.indexprovider.SpatialIndexProvider;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpatialService {
    private GraphDatabaseService graphDatabaseService;
    private Index<Node> spatialIndex = null;

    public SpatialService(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
    }

    public List<Station> reorderNearestStations(Double latitude, Double longitude, List<Station> stations) {
        Transaction tx = graphDatabaseService.beginTx();
        try {
            stations.sort((s1, s2) -> s1.getName().compareTo(s2.getName()));
            List<Node> nearestStations = getNearestStationsTo(latitude, longitude, 10);
            List<Station> reorderedStations = new ArrayList<>();
            int count = 0;

            for (Node node : nearestStations) {
                String id = node.getProperty(GraphStaticKeys.ID).toString();
                Station nearestStation = getStation(stations, id);
                if (nearestStation != null) {
                    nearestStation.setProximityGroup("Nearest Stops");
                    reorderedStations.add(nearestStation);
                    count++;
                }
                if (count >= 6)
                    break;
            }

            for (Station station : stations) {
                if (reorderedStations.contains(station) == false) {
                    station.setProximityGroup("All Stops");
                    reorderedStations.add(station);
                }
            }
            tx.success();
            return reorderedStations;
        } finally {
            tx.close();
        }
    }

    private Station getStation(List<Station> stations, String id) {
        for (Station station : stations) {
            if (station.getId().equals(id))
                return station;
        }

        return null;
    }

    private List<Node> getNearestStationsTo(double latitude, double longitude, int count) {
        Map<String, Object> params = new HashMap<>();
        params.put(LayerNodeIndex.POINT_PARAMETER, new Double[]{latitude, longitude});
        params.put(LayerNodeIndex.DISTANCE_IN_KM_PARAMETER, 100.0);
        IndexHits<Node> query = getSpatialIndex().query(LayerNodeIndex.WITHIN_DISTANCE_QUERY, params);
        List<Node> nearestNodes = new ArrayList<>();
        int addedCount = 0;

        while (query.hasNext() && addedCount < count) {
            nearestNodes.add(query.next());
            addedCount++;
        }
        return nearestNodes;
    }

    private Index<Node> getSpatialIndex() {
        if (spatialIndex == null)
            spatialIndex = graphDatabaseService.index().forNodes("spatial_index", SpatialIndexProvider.SIMPLE_POINT_CONFIG);

        return spatialIndex;
    }
}
