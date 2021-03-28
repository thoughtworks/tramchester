package com.tramchester.graph;

import com.tramchester.domain.reference.TransportMode;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import java.util.*;
import java.util.stream.Collectors;

public enum TransportRelationshipTypes implements RelationshipType {
    TRAM_GOES_TO,
    BUS_GOES_TO,
    TRAIN_GOES_TO,
    FERRY_GOES_TO,
    SUBWAY_GOES_TO,

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
    ON_ROUTE,
    TRAM_NEIGHBOUR,
    BUS_NEIGHBOUR,
    TRAIN_NEIGHBOUR,
    LINKED;

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

    public static TransportRelationshipTypes forMode(TransportMode transportMode) {
        return switch (transportMode) {
            case Train -> TRAIN_GOES_TO;
            case Bus, RailReplacementBus -> BUS_GOES_TO;
            case Tram -> TRAM_GOES_TO;
            case Ferry -> FERRY_GOES_TO;
            case Subway -> SUBWAY_GOES_TO;
            default -> throw new RuntimeException("Unexpected travel mode " + transportMode);
        };
    }

    public static boolean isNeighbour(Relationship relationship) {
        return relationship.isType(TRAM_NEIGHBOUR) ||
                relationship.isType(TRAIN_NEIGHBOUR) ||
                relationship.isType(BUS_NEIGHBOUR);
    }

    public static boolean hasCost(TransportRelationshipTypes relationshipType) {
        return switch (relationshipType) {
            case TO_HOUR,TO_MINUTE, TO_SERVICE -> false;
            default -> true;
        };
    }

    public static boolean hasTripId(TransportRelationshipTypes relationshipType) {
        return switch (relationshipType) {
            case TRAM_GOES_TO, TRAIN_GOES_TO, BUS_GOES_TO, FERRY_GOES_TO, SUBWAY_GOES_TO, TO_MINUTE -> true;
            default -> false;
        };
    }

    public static Set<TransportRelationshipTypes> haveCosts() {
        return Arrays.stream(values()).filter(TransportRelationshipTypes::hasCost).collect(Collectors.toSet());
    }

    public static Set<TransportRelationshipTypes> haveTripId() {
        return Arrays.stream(values()).filter(TransportRelationshipTypes::hasTripId).collect(Collectors.toSet());
    }

}

