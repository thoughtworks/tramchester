package com.tramchester.graph.graphbuild;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.*;
import org.neo4j.graphdb.*;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;

import static com.tramchester.graph.GraphStaticKeys.*;
import static com.tramchester.graph.GraphStaticKeys.HOUR;
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


    @Override
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

    @Override
    public void stop() {
        // no op
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

    protected void setProp(Node node, DataSourceInfo.NameAndVersion nameAndVersion) {
        node.setProperty(nameAndVersion.getName(), nameAndVersion.getVersion());
    }

    protected void setProp(Entity entity, LocalTime time) {
        entity.setProperty(TIME, time);
    }

    protected void setTripsProp(Relationship relationship, String value) {
        relationship.setProperty(TRIPS, value);
    }

    @Deprecated
    protected void setIdProp(Node node, String value) {
        node.setProperty(ID, value);
    }

    protected void setIdPropForPlatform(Node node, IdFor<Platform> id) {
        node.setProperty(ID, id.getGraphId());
    }

    protected void setIdPropForStation(Node node, IdFor<Station> id) {
        node.setProperty(ID, id.getGraphId());
    }

    protected void setRouteProp(Entity entity, IdFor<Route> id) {
        entity.setProperty(ROUTE_ID, id.getGraphId());
    }

    protected void setServiceProp(Entity entity, IdFor<Service> id) {
        entity.setProperty(SERVICE_ID, id.getGraphId());
    }

    protected void setTripProp(Entity entity, IdFor<Trip> id) {
        entity.setProperty(TRIP_ID, id.getGraphId());
    }

    protected void setPlatformProp(Entity entity, IdFor<Platform> id) {
        entity.setProperty(PLATFORM_ID, id.getGraphId());
    }

    protected void setStationProp(Entity entity, IdFor<Station> id) {
        entity.setProperty(STATION_ID, id.getGraphId());
    }

    protected void setRouteStationProp(Entity entity, IdFor<RouteStation> id) {
        entity.setProperty(ROUTE_STATION_ID, id.getGraphId());
    }

    protected void setTowardsProp(Node node, IdFor<Station> id) {
        node.setProperty(TOWARDS_STATION_ID, id.getGraphId());
    }

    protected void setCostProp(Relationship relationship, int value) {
        relationship.setProperty(COST, value);
    }

    protected void setHourProp(Entity entity, Integer value) {
        entity.setProperty(HOUR, value);
    }
}
