package com.tramchester.graph;

import com.tramchester.graph.graphbuild.GraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

public interface NodeTypeRepository {
    boolean isTime(Node node);
    boolean isHour(Node node);
    boolean isBusStation(Node node);
    boolean isRouteStation(Node node);
    boolean isService(Node node);

    Node createQueryNode(GraphDatabase graphDatabase, Transaction txn);
    Node createQueryNodeMidPoint(GraphDatabase graphDatabase, Transaction txn);
    void deleteQueryNode(Node queryNode);

    void populateNodeLabelMap(GraphDatabase graphDatabase);
    void put(long id, GraphBuilder.Labels label);
}
