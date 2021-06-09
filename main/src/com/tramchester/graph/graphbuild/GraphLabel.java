package com.tramchester.graph.graphbuild;

import com.tramchester.domain.reference.TransportMode;
import org.neo4j.graphdb.Label;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum GraphLabel implements Label {
    GROUPED,  // composite station node
    ROUTE_STATION,
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

    public static boolean isStation(GraphLabel label) {
        return label == TRAM_STATION || isNoPlatformStation(label);
    }

    /**
     * should be based on data source config only
     * @param label the graph label
     * @return true if label is TRAM_STATION
     */
    @Deprecated
    public static boolean isNoPlatformStation(GraphLabel label) {
        return label == BUS_STATION || label == TRAIN_STATION || label == FERRY_STATION
                || label == SUBWAY_STATION;
    }

    public static Set<GraphLabel> from(Iterable<Label> labels) {
        Set<GraphLabel> result = new HashSet<>();
        labels.forEach(label -> result.add(valueOf(label.name())));
        return result;
    }

}
