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

    // journey planning
    BOARD,
    DEPART,
    INTERCHANGE_BOARD,
    INTERCHANGE_DEPART,
    WALKS_TO,
    WALKS_FROM,
    ENTER_PLATFORM,
    LEAVE_PLATFORM,
    TO_SERVICE,
    TO_HOUR,
    TO_MINUTE,

    // between composite stations to/from contained stations
    GROUPED_TO_PARENT,
    GROUPED_TO_CHILD,

    // routes, allow faster initial estimation of cost and 'hop' counts while traversing minimal number of nodes
    ON_ROUTE,  // links route stations on same route
    ROUTE_TO_STATION, // link stations and routes irrespective of whether have platforms or not
    STATION_TO_ROUTE,

    NEIGHBOUR, // stations within N meters, different transport modes
    LINKED; // station to station by transport mode, supports network visualisation and analysis

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

    public static TransportRelationshipTypes from(Relationship relationship) {
        return valueOf(relationship.getType().name());
    }
}

