package com.tramchester.graph.graphbuild;

import com.tramchester.domain.reference.TransportMode;
import org.neo4j.graphdb.Label;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum GraphLabel implements Label {
    GROUPED,  // composite station node
    ROUTE_STATION,
    STATION, // any station node must have this label
    TRAM_STATION,
    BUS_STATION,
    TRAIN_STATION,
    FERRY_STATION,
    SUBWAY_STATION,
    PLATFORM,
    QUERY_NODE,
    SERVICE,
    HOUR,
    MINUTE,
    VERSION,
    NEIGHBOURS_ENABLED,
    COMPOSITES_ADDED,
    HAS_PLATFORMS, // label added to stations if have platforms
    INTERCHANGE; // label added to stations if they are interchanges

    public static GraphLabel forMode(TransportMode mode) {
        return switch (mode) {
            case Tram -> TRAM_STATION;
            case Bus -> BUS_STATION;
            case Train, RailReplacementBus -> TRAIN_STATION;
            case Ferry -> FERRY_STATION;
            case Subway -> SUBWAY_STATION;
            case Walk -> QUERY_NODE;
            default -> throw new RuntimeException("Unsupported mode " + mode);
        };
    }

    public static Set<GraphLabel> forMode(Set<TransportMode> modes) {
        return modes.stream().map(mode -> forMode(mode.getTransportMode())).collect(Collectors.toSet());
    }

    public static Set<GraphLabel> from(Iterable<Label> labels) {
        Set<GraphLabel> result = new HashSet<>();
        labels.forEach(label -> result.add(valueOf(label.name())));
        return result;
    }

    public static boolean isStation(Iterable<Label> labels) {
        for (Label label : labels) {
            if (label.name().equals(STATION.name())) {
                return true;
            }
        }
        return false;
    }
}
