package com.tramchester.graph;

import com.tramchester.graph.graphbuild.GraphBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.Set;

public interface NodeTypeRepository {
    boolean isTime(Node node);
    boolean isHour(Node node);
    boolean isBusStation(Node node);
    boolean isRouteStation(Node node);
    boolean isService(Node node);
    boolean isTrainStation(Node node);

    Node createQueryNode(GraphDatabase graphDatabase, Transaction txn);
    void deleteQueryNode(Node queryNode);

    void populateNodeLabelMap(GraphDatabase graphDatabase);
    void put(long id, GraphBuilder.Labels label);
    void put(long id, Set<GraphBuilder.Labels> label);

    boolean shouldContain(GraphBuilder.Labels label);
}
