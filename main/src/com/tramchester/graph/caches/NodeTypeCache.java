package com.tramchester.graph.caches;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.NumberOfNodesAndRelationshipsRepository;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.metrics.TimedTransaction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.tramchester.graph.graphbuild.GraphBuilder.Labels.*;

@LazySingleton
public class NodeTypeCache implements NodeTypeRepository {
    private static final Logger logger = LoggerFactory.getLogger(NodeTypeCache.class);

    private final ConcurrentMap<GraphBuilder.Labels, Set<Long>> labelMap;
    private final ConcurrentMap<Long, Boolean> queryNodes;

    private final Set<GraphBuilder.Labels> nodesToCache = new HashSet<>(Arrays.asList(
            ROUTE_STATION, PLATFORM, SERVICE, HOUR, MINUTE, TRAM_STATION, BUS_STATION, TRAIN_STATION,
            FERRY_STATION, SUBWAY_STATION
    ));
    private final GraphDatabase graphDatabase;
    private final NumberOfNodesAndRelationshipsRepository numbersOfNodes;

    // TODO use NumberOfNodesAndRelationshipsRepository here
    @Inject
    public NodeTypeCache(GraphDatabase graphDatabase, NumberOfNodesAndRelationshipsRepository numbersOfNodes,
                         StagedTransportGraphBuilder.Ready ready) {
        this.graphDatabase = graphDatabase;
        this.numbersOfNodes = numbersOfNodes;
        labelMap = new ConcurrentHashMap<>();

        for (GraphBuilder.Labels label: nodesToCache) {
            labelMap.put(label, new HashSet<>(getCapacity(label), 0.8F));
        }
        queryNodes = new ConcurrentHashMap<>();
    }

    @PostConstruct
    private void start() {
        logger.info("start");
        populateCaches();
        logger.info("started");
    }

    @PreDestroy
    public void dispose() {
        logger.info("dispose");
        if (!queryNodes.isEmpty()) {
            logger.warn("Query nodes remaining " + queryNodes.keySet());
            queryNodes.clear();
        }
        labelMap.clear();
    }

    private void populateCaches() {
        // populate
        try (TimedTransaction timed = new TimedTransaction(graphDatabase, logger, "populate node type cache")) {
            Transaction tx = timed.transaction();
            for (GraphBuilder.Labels label : nodesToCache) {
                graphDatabase.findNodes(tx, label).stream().forEach(node -> put(node.getId(), label));
            }
        }

        // logging for diagnostics
        for (GraphBuilder.Labels label : nodesToCache) {
            int size = labelMap.get(label).size();
            if (size>0) {
                logger.info("Loaded " + size + " for label " + label);
            } else {
                logger.info("Loaded zero nodes for label " + label);
            }
        }
    }


    private int getCapacity(GraphBuilder.Labels label) {
        return numbersOfNodes.numberOf(label).intValue();
        // approx. sizings
//        return switch (label) {
//            case ROUTE_STATION -> 282;
//            case TRAM_STATION -> 93;
//            case TRAIN_STATION -> 3000;
//            case PLATFORM -> 185;
//            case SERVICE -> 8680;
//            case HOUR -> 62525;
//            case MINUTE -> 314150;
//            default -> 0;
//        };
    }

    public void put(long id, GraphBuilder.Labels label) {
        labelMap.get(label).add(id);
    }

    public void put(long id, Set<GraphBuilder.Labels> labels) {
        labels.forEach(label -> put(id, label));
    }

    @Override
    public boolean isService(Node nodeId) {
        return has(SERVICE, nodeId.getId());
    }

    @Override
    public boolean isTrainStation(Node node) {
        return has(TRAIN_STATION, node.getId());
    }

    @Override
    public boolean isHour(Node node) {
        return has(HOUR, node.getId());
    }

    @Override
    public boolean isTime(Node node) { return has(MINUTE, node.getId()); }

    @Override
    public boolean isRouteStation(Node node) {
        return has(ROUTE_STATION, node.getId());
    }

    @Override
    public boolean isBusStation(Node node) { return has(BUS_STATION, node.getId()); }

    private boolean has(final GraphBuilder.Labels label, final long nodeId) {
        if (label == GraphBuilder.Labels.QUERY_NODE) {
            return queryNodes.containsKey(nodeId);
        }
        return labelMap.get(label).contains(nodeId);
    }

    // for creating query nodes, to support MyLocation journeys
    @Override
    public Node createQueryNode(GraphDatabase graphDatabase, Transaction txn) {
        Node result = graphDatabase.createNode(txn, GraphBuilder.Labels.QUERY_NODE);
        queryNodes.put(result.getId(), true);
        return result;
    }

    // for deleting query nodes, to support MyLocation journeys
    @Override
    public void deleteQueryNode(Node node) {
        long id = node.getId();
        node.delete();
        queryNodes.remove(id);
    }

    @Override
    public String toString() {
        return "NodeIdLabelMap{" +
                "labelMap=" + labelMap +
                ", queryNodes=" + queryNodes +
                ", nodesToCache=" + nodesToCache +
                '}';
    }
}
