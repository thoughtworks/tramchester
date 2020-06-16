package com.tramchester.graph;

import org.neo4j.graphdb.RelationshipType;

import java.util.*;

public enum TransportRelationshipTypes implements RelationshipType {
    TRAM_GOES_TO,
    BUS_GOES_TO,
    BOARD,
    DEPART,
    INTERCHANGE_BOARD,
    INTERCHANGE_DEPART,
    WALKS_TO,
    WALKS_FROM,
    FINISH_WALK,
    ENTER_PLATFORM,
    LEAVE_PLATFORM,
    TO_SERVICE,
    TO_HOUR,
    TO_MINUTE,
    ON_ROUTE;

    private static final TransportRelationshipTypes[] forPlanning;

    static {
        Set<TransportRelationshipTypes> all = new HashSet<>(Arrays.asList(TransportRelationshipTypes.values()));
        all.remove(ON_ROUTE); // not used for traversals
        forPlanning = new TransportRelationshipTypes[all.size()];
        all.toArray(forPlanning);
    }

    public static TransportRelationshipTypes[] forPlanning() {
        return forPlanning;
    }

}

