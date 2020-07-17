package com.tramchester.graph.graphbuild;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.graph.*;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tramchester.graph.GraphStaticKeys.ROUTE_ID;
import static com.tramchester.graph.GraphStaticKeys.STATION_ID;
import static java.lang.String.format;

public abstract class GraphBuilder implements Startable {
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

    public enum Labels implements Label
    {
        ROUTE_STATION, TRAM_STATION, BUS_STATION, TRAIN_STATION, PLATFORM, QUERY_NODE, SERVICE, HOUR, MINUTE, VERSION, QUERY_NODE_MID;

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

    }

    protected final TramchesterConfig config;
    private final GraphFilter graphFilter;
    private final GraphDatabase graphDatabase;
    private final NodeTypeRepository nodeIdLabelMap;
    protected final GraphQuery graphQuery;

    private int numberNodes;
    private int numberRelationships;

    protected GraphBuilder(GraphDatabase graphDatabase, GraphQuery graphQuery, GraphFilter graphFilter, TramchesterConfig config,
                           NodeTypeRepository nodeIdLabelMap) {
        this.graphDatabase = graphDatabase;
        this.graphQuery = graphQuery;
        this.config = config;
        this.graphFilter = graphFilter;
        this.nodeIdLabelMap = nodeIdLabelMap;
        numberNodes = 0;
        numberRelationships = 0;
    }

    @NotNull
    protected Node createRouteStationNode(Transaction tx, Location station, Route route) {
        Node routeStation = createGraphNode(tx, Labels.ROUTE_STATION);
        String routeStationId = RouteStation.formId(station, route);

        logger.debug(format("Creating route station %s route %s nodeId %s", station.getId(),route.getId(),
                routeStation.getId()));
        routeStation.setProperty(GraphStaticKeys.ID, routeStationId);
        routeStation.setProperty(STATION_ID, station.getId());
        routeStation.setProperty(ROUTE_ID, route.getId());
        return routeStation;
    }

    @NotNull
    protected Node createStationNode(Transaction tx, Location station) {
        String id = station.getId();

        Labels label = Labels.forMode(station.getTransportMode());
        logger.debug(format("Creating station node: %s with label: %s ", station, label));
        Node stationNode = createGraphNode(tx, label);
        stationNode.setProperty(GraphStaticKeys.ID, id);
        return stationNode;
    }

    @Override
    public void start() {
        logger.info("start");
        if (config.getRebuildGraph()) {
            logger.info("Rebuild of graph DB for " + config.getGraphName());
            if (graphFilter.isFiltered()) {
                buildGraphwithFilter(graphFilter, graphDatabase);
            } else {
                buildGraph(graphDatabase);
            }
            logger.info("Graph rebuild is finished for " + config.getGraphName());
        } else {
            logger.info("No rebuild of graph, using existing data");
            loadGraph();
        }
    }

    protected abstract void buildGraph(GraphDatabase graphDatabase);

    protected abstract void buildGraphwithFilter(GraphFilter graphFilter, GraphDatabase graphDatabase);

    @Override
    public void stop() {
        // no op
    }

    private void loadGraph() {
        nodeIdLabelMap.populateNodeLabelMap(graphDatabase);
    }

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
