package com.tramchester.graph.graphbuild;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.*;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public abstract class GraphBuilder  {
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

    public static class Ready {
        protected Ready() {
            // prevent guice creating this, want to create dependency on the Guilder
        }
    }

    public enum Labels implements Label
    {
        ROUTE_STATION,
        TRAM_STATION,
        BUS_STATION,
        TRAIN_STATION,
        PLATFORM,
        QUERY_NODE,
        SERVICE,
        HOUR,
        MINUTE,
        VERSION,
        NEIGHBOURS_ENABLED;

        public static Labels forMode(TransportMode mode) {
            switch (mode) {
                case Tram:
                    return TRAM_STATION;
                case Bus:
                    return BUS_STATION;
                case Train:
                    return TRAIN_STATION;
                default:
                    throw new RuntimeException("Unsupported mode " + mode);
            }
        }

        public static boolean isStation(Labels label) {
            return label==TRAM_STATION || label==BUS_STATION || label==TRAIN_STATION;
        }
    }

    private final TramchesterConfig config;
    private final GraphFilter graphFilter;
    protected final GraphDatabase graphDatabase;
    private final NodeTypeRepository nodeIdLabelMap;

    private int numberNodes;
    private int numberRelationships;

    protected GraphBuilder(GraphDatabase graphDatabase, GraphFilter graphFilter, TramchesterConfig config,
                           NodeTypeRepository nodeIdLabelMap) {
        this.graphDatabase = graphDatabase;
        this.config = config;
        this.graphFilter = graphFilter;
        this.nodeIdLabelMap = nodeIdLabelMap;
        numberNodes = 0;
        numberRelationships = 0;
    }

    public void start() {
        logger.info("start");
        if (graphDatabase.isCleanDB()) {
            logger.info("Rebuild of graph DB for " + config.getGraphName());
            if (graphFilter.isFiltered()) {
                buildGraphwithFilter(graphFilter, graphDatabase);
            } else {
                buildGraph(graphDatabase);
            }
            logger.info("Graph rebuild is finished for " + config.getGraphName());
        } else {
            logger.info("No rebuild of graph, using existing data");
            nodeIdLabelMap.populateNodeLabelMap(graphDatabase);
        }
    }

    protected abstract void buildGraph(GraphDatabase graphDatabase);

    protected abstract void buildGraphwithFilter(GraphFilter graphFilter, GraphDatabase graphDatabase);

    protected Node createGraphNode(Transaction tx, Labels label) {
        numberNodes++;
        Node node = graphDatabase.createNode(tx, label);
        nodeIdLabelMap.put(node.getId(), label);
        return node;
    }

    protected Relationship createRelationship(Node start, Node end, TransportRelationshipTypes relationshipType) {
        numberRelationships++;
        return start.createRelationshipTo(end, relationshipType);
    }

    protected void logMemory(String prefix) {
        logger.warn(format("MemoryUsage %s free:%s total:%s ", prefix,
                Runtime.getRuntime().freeMemory(), Runtime.getRuntime().totalMemory()));
    }

    protected void reportStats() {
        logger.info("Nodes created: " + numberNodes);
        logger.info("Relationships created: " + numberRelationships);
    }
}
