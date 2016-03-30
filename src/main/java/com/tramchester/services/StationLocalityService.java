package com.tramchester.services;

import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.StationIndexs;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.repository.TransportData;
import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class StationLocalityService extends StationIndexs {
    private static final Logger logger = LoggerFactory.getLogger(StationLocalityService.class);
    private TransportData transportData;
    private double distanceInKM = 0.01;
    private int assumedCost = 1;

    public StationLocalityService(GraphDatabaseService graphDatabaseService, RelationshipFactory relationshipFactory,
                                  TransportData transportData) {
        super(graphDatabaseService, relationshipFactory, false);
        this.transportData = transportData;
    }

    public void populateLocality() {
        Map<String, Object> params = new HashMap<>();
        params.put(LayerNodeIndex.DISTANCE_IN_KM_PARAMETER, distanceInKM);

        logger.info("Populating bus stop locallity information");
        try (Transaction tx = graphDatabaseService.beginTx()) {
            transportData.getStations().stream().filter(station -> !station.isTram()).forEach(busStop -> {
                Node busNode = super.getStationNode(busStop.getId());
                LatLong latLong = new LatLong(busStop.getLatitude(), busStop.getLongitude());
                params.put(LayerNodeIndex.POINT_PARAMETER, new Double[]{latLong.getLat(), latLong.getLon()});

                IndexHits<Node> query = getSpatialIndex().query(LayerNodeIndex.WITHIN_DISTANCE_QUERY, params);
                query.forEach(node -> busNode.createRelationshipTo(node, TransportRelationshipTypes.WALKS_TO).
                        setProperty(GraphStaticKeys.COST, assumedCost));
            });
            tx.close();
        }
        logger.info("Finished adding locality information");
    }
}
