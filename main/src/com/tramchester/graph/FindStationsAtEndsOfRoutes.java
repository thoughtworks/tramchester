package com.tramchester.graph;

import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

public class FindStationsAtEndsOfRoutes {

    public IdSet<Station> searchFor(TransportMode mode) {
        return null;
    }


//    MATCH (a:A)
//    WHERE NOT (a)-[*]->(:B {prop:false})
//    RETURN a;

//    MATCH (a:A)
//    OPTIONAL MATCH (a)-[*]->(b:B { prop: false })
//    WITH a, COLLECT(b) AS bs
//    WHERE SIZE(bs)= 0
//    RETURN a;

//    @Deprecated
//    public IdSet<Station> findFor(TransportMode mode, int threshhold) {
//        logger.info(format("Find for %s threshhold %s", mode, threshhold));
//        long start = System.currentTimeMillis();
//        Map<String, Object> params = new HashMap<>();
//        String stationLabel = GraphBuilder.Labels.forMode(mode).name();
//
//        params.put("count", threshhold);
//        params.put("mode", mode.getNumber());
//        String query = format("MATCH (a:%s)-[r:LINKED]->(b) " +
//                        "WHERE $mode in r.transport_modes " +
//                        "WITH a, count(r) as num " +
//                        "WHERE num=$count " +
//                        "RETURN a", stationLabel);
//        logger.info("Query: '" + query + '"');
//
//        IdSet<Station> stationIds = new IdSet<>();
//        try (Transaction txn  = graphDatabase.beginTx()) {
//            Result result = txn.execute(query, params);
//            while (result.hasNext()) {
//                Map<String, Object> row = result.next();
//                Node node = (Node) row.get("a");
//                stationIds.add(GraphProps.getStationId(node));
//            }
//            result.close();
//        }
//        long duration = System.currentTimeMillis()-start;
//        logger.info("Took " + duration);
//        logger.info("Found " + stationIds.size() + " matches");
//        return stationIds;
//    }

}
