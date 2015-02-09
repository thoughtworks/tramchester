package com.tramchester.graph;

import org.neo4j.graphdb.RelationshipType;

public enum TransportRelationshipTypes implements RelationshipType {
    PLATFORM, OPERATES_ON, GOES_TO, BOARD, STATION, DEPART
}
