package com.tramchester.graph.Nodes;

import org.neo4j.graphdb.Relationship;

import java.util.Set;

public interface TramNode {
    boolean isStation();
    boolean isRouteStation();

    String getId();

    Set<Relationship> getRelationships();
}
