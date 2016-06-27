package com.tramchester.graph;


import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Location;
import com.tramchester.domain.StationWalk;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.repository.StationRepository;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

public class TransportGraphAddWalkingRoutes extends StationIndexs  {

    private static final String DEANSGATE = "9400ZZMAGMX";
    private static final String MARKET_STREET = "9400ZZMAMKT";
    private static final int COST = 17;
    private TramchesterConfig config;

    public TransportGraphAddWalkingRoutes(GraphDatabaseService graphDatabaseService, RelationshipFactory relationshipFactory,
                                          SpatialDatabaseService spatialDatabaseService,
                                          TramchesterConfig config) {
        super(graphDatabaseService, relationshipFactory, spatialDatabaseService, true);
        this.config = config;
    }

    public void addCityCentreWalkingRoutes() {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Node deansgate = super.getStationNode(DEANSGATE);
            Node marketStreet = super.getStationNode(MARKET_STREET);

            deansgate.createRelationshipTo(marketStreet,TransportRelationshipTypes.WALKS_TO).
                    setProperty(GraphStaticKeys.COST, COST);

            tx.success();
            tx.close();
        }
    }

    private int findCostInMinutes(Location startionA, Location stationB) {
        LatLng point1 = LatLong.getLatLng(startionA.getLatLong());
        LatLng point2 = LatLong.getLatLng(stationB.getLatLong());

        double distanceInMiles = LatLngTool.distance(point1, point2, LengthUnit.MILE);
        double hours = distanceInMiles / config.getWalkingMPH();
        return (int)Math.ceil(hours * 60D);
    }
}
