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
