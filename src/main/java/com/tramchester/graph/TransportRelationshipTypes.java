package com.tramchester.graph;

import org.neo4j.graphdb.RelationshipType;

public enum TransportRelationshipTypes implements RelationshipType {
    GOES_TO,
    BOARD,
    DEPART,
    INTERCHANGE
}
