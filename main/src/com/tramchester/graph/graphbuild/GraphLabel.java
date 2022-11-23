package com.tramchester.graph.graphbuild;

import com.tramchester.domain.reference.TransportMode;
import org.neo4j.graphdb.Label;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public enum GraphLabel implements Label {
    GROUPED,  // grouped station node
    ROUTE_STATION,
    STATION, // any station node must have this label
    HAS_PLATFORMS, // label added to stations if have platforms
    INTERCHANGE, // label added to stations if they are interchanges
    // transport modes - added to route stations
    TRAM,
    BUS,
    TRAIN,
    FERRY, // aka a port
    SUBWAY,

    PLATFORM,
    QUERY_NODE, // created to support journeys walking from arbitary location
    SERVICE,
    HOUR,
    MINUTE,
    // meta labels for versions and feature flags
    VERSION,
    NEIGHBOURS_ENABLED,
    WALK_FOR_CLOSED_ENABLED,
    COMPOSITES_ADDED,

    // Order for HOUR_N matters, used in sorting
    HOUR_0, HOUR_1, HOUR_2, HOUR_3, HOUR_4, HOUR_5, HOUR_6, HOUR_7,
    HOUR_8, HOUR_9, HOUR_10, HOUR_11, HOUR_12, HOUR_13, HOUR_14, HOUR_15,
    HOUR_16, HOUR_17, HOUR_18, HOUR_19, HOUR_20, HOUR_21, HOUR_22, HOUR_23;

    private static final GraphLabel[] hourLabels;

    static {
        hourLabels = new GraphLabel[24];
        for (int hour = 0; hour < 24; hour++) {
            hourLabels[hour] = GraphLabel.valueOf(format("HOUR_%d", hour));
        }
    }

    public static GraphLabel forMode(TransportMode mode) {
        return switch (mode) {
            case Tram -> TRAM;
            case Bus -> BUS;
            case Train, RailReplacementBus -> TRAIN;
            case Ferry, Ship -> FERRY;
            case Subway -> SUBWAY;
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

    public static Label getHourLabel(int hour) {
        return hourLabels[hour];
    }

    public static int getHourFrom(EnumSet<GraphLabel> labels) {
        for (int hour = 0; hour < 24    ; hour++) {
            if (labels.contains(hourLabels[hour])) {
                return hour;
            }
        }
        throw new RuntimeException("Could not find hour from " + labels);
    }
}
