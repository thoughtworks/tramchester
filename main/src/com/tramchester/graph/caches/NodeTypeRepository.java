package com.tramchester.graph.caches;

import com.tramchester.graph.GraphDatabase;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

public interface NodeTypeRepository {
    boolean isTime(Node node);
    boolean isHour(Node node);
    boolean isRouteStation(Node node);
    boolean isService(Node node);

    boolean isTrainStation(Node node);
    boolean isBusStation(Node node);

    Node createQueryNode(GraphDatabase graphDatabase, Transaction txn);
    void deleteQueryNode(Node queryNode);

}
