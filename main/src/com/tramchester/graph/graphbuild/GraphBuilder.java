package com.tramchester.graph.graphbuild;

import com.tramchester.config.GraphDBConfig;
import com.tramchester.config.HasGraphDBConfig;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.filters.GraphFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public abstract class GraphBuilder extends CreateNodesAndRelationships {
    private static final Logger logger = LoggerFactory.getLogger(GraphBuilder.class);

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
