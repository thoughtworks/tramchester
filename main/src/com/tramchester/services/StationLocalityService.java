package com.tramchester.services;

import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.StationIndexs;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.repository.TransportData;
import com.vividsolutions.jts.geom.Coordinate;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.pipes.GeoPipeFlow;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class StationLocalityService extends StationIndexs {
    private static final Logger logger = LoggerFactory.getLogger(StationLocalityService.class);
    private TransportData transportData;
    private double distanceInKM = 0.2;
    private int assumedCost = 1;

    public StationLocalityService(GraphDatabaseService graphDatabaseService, RelationshipFactory relationshipFactory,
                                  TransportData transportData, SpatialDatabaseService spatialDatabaseService) {
        super(graphDatabaseService, relationshipFactory, spatialDatabaseService, false);
        this.transportData = transportData;
    }


    public void populateLocality() {

        logger.info("Populating bus stop locallity information");
        try (Transaction tx = graphDatabaseService.beginTx()) {
            transportData.getStations().stream().filter(station -> !station.isTram()).forEach(busStop -> {

                Coordinate coordinate = LatLong.getCoordinate(busStop.getLatLong());
                List<GeoPipeFlow> results = getSpatialLayer().findClosestPointsTo(coordinate, distanceInKM);
                if (results.size()>1) {
                    addNearbyStops(busStop, results);
                }
            });
        }
        logger.info("Finished adding locality information");
    }

    private void addNearbyStops(Station busStop,List<GeoPipeFlow> nearbyStops) {
        String busStopId = busStop.getId();

        logger.info("Found " + nearbyStops.size() + " nodes for " + busStopId);
        Node busNode = super.getStationNode(busStopId);
        nearbyStops.forEach(nearby -> {
            Node node = nearby.getRecord().getGeomNode();
            if (busNode.getId() != node.getId()) {
                createRelationship(busNode, node);
            }
        });
    }

    private void createRelationship(Node busNode, Node nearby) {
        busNode.createRelationshipTo(nearby, TransportRelationshipTypes.WALKS_TO).
                setProperty(GraphStaticKeys.COST, assumedCost);

    }

}
