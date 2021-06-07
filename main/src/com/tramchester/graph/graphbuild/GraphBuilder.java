package com.tramchester.graph.graphbuild;

import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.HasGraphDBConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.filters.GraphFilter;
import org.neo4j.graphdb.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public abstract class GraphBuilder extends CreateNodesAndRelationships {
    private static final Logger logger = LoggerFactory.getLogger(GraphBuilder.class);

    protected static final int INTERCHANGE_DEPART_COST = 1;
    protected static final int INTERCHANGE_BOARD_COST = 1;
    protected static final int DEPARTS_COST = 1;
    protected static final int BOARDING_COST = 2;
    // TODO compute actual costs depend on physical configuration of platforms at the station? No data available yet.
    protected static final int ENTER_PLATFORM_COST = 0;
    protected static final int LEAVE_PLATFORM_COST = 0;
    protected static final int ENTER_INTER_PLATFORM_COST = 0;
    protected static final int LEAVE_INTER_PLATFORM_COST = 0;

    // TODO Push up
    public enum Labels implements Label
    {
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

        public static Labels forMode(TransportMode mode) {
            return switch (mode) {
                case Tram -> TRAM_STATION;
                case Bus -> BUS_STATION;
                case Train, RailReplacementBus -> TRAIN_STATION;
                case Ferry -> FERRY_STATION;
                case Subway -> SUBWAY_STATION;
                default -> throw new RuntimeException("Unsupported mode " + mode);
            };
        }

        public static Set<Labels> forMode(Set<TransportMode> modes) {
            return modes.stream().map(mode -> forMode(mode.getTransportMode())).collect(Collectors.toSet());
        }

        public static boolean isStation(Labels label) {
            return label == TRAM_STATION || isNoPlatformStation(label);
        }

        public static boolean isStation(Iterable<Label> nodeLabels) {
            for(Label nodeLabel : nodeLabels) {
                if (isStation(valueOf(nodeLabel.toString()))) {
                    return true;
                }
            }
            return false;
        }

        public static boolean isNoPlatformStation(Labels label) {
            return label == BUS_STATION || label == TRAIN_STATION || label == FERRY_STATION
                    || label == SUBWAY_STATION;
        }

        public static Set<Labels> from(Iterable<Label> labels) {
            Set<Labels> result = new HashSet<>();
            labels.forEach(label -> result.add(valueOf(label.toString())));
            return result;
        }

    }

    protected final GraphDBConfig graphDBConfig;
    protected final GraphFilter graphFilter;
    protected final GraphBuilderCache builderCache;

    protected GraphBuilder(GraphDatabase graphDatabase, GraphFilter graphFilter, HasGraphDBConfig config,
                           GraphBuilderCache builderCache) {
        super(graphDatabase);
        this.graphDBConfig = config.getGraphDBConfig();
        this.graphFilter = graphFilter;
        this.builderCache = builderCache;
    }

    protected void logMemory(String prefix) {
        logger.warn(format("MemoryUsage %s free:%s total:%s ", prefix,
                Runtime.getRuntime().freeMemory(), Runtime.getRuntime().totalMemory()));
    }

}
