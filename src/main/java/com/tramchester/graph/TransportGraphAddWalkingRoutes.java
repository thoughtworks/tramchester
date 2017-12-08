package com.tramchester.graph;


import com.tramchester.domain.input.Interchanges;
import com.tramchester.graph.Relationships.RelationshipFactory;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.Arrays;
import java.util.List;

public class TransportGraphAddWalkingRoutes extends StationIndexs  {

    private static final String MARKET_STREET = "9400ZZMAMKT";
    private static final String EXCHANGE_SQUARE = "9400ZZMAEXS";

    private List<Walk> walks = Arrays.asList(
            walk(Interchanges.DEANSGATE, MARKET_STREET, 17),
            walk(Interchanges.DEANSGATE, Interchanges.PIC_GARDENS, 17),
            walk(Interchanges.DEANSGATE, Interchanges.PICCADILLY, 19),
            walk(Interchanges.DEANSGATE, EXCHANGE_SQUARE, 19),
            walk(Interchanges.DEANSGATE, Interchanges.VICTORIA, 22)
    );

    public TransportGraphAddWalkingRoutes(GraphDatabaseService graphDatabaseService, RelationshipFactory relationshipFactory,
                                          SpatialDatabaseService spatialDatabaseService) {
        super(graphDatabaseService, relationshipFactory, spatialDatabaseService, true);
    }

    public void addCityCentreWalkingRoutes() {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            walks.forEach(walk -> {
                Node first = super.getStationNode(walk.begin);
                Node second = super.getStationNode(walk.end);
                first.createRelationshipTo(second,TransportRelationshipTypes.WALKS_TO).
                        setProperty(GraphStaticKeys.COST, walk.minutes);
                second.createRelationshipTo(first, TransportRelationshipTypes.WALKS_TO).
                        setProperty(GraphStaticKeys.COST, walk.minutes);
            });

            tx.success();
        }
    }

    private static Walk walk(String begin, String end, int minutes) {
        return new Walk(begin,end,minutes);
    }

    private static class Walk {

        private String begin;
        private String end;
        private int minutes;

        public Walk(String begin, String end, int minutes) {

            this.begin = begin;
            this.end = end;
            this.minutes = minutes;
        }
    }

}
