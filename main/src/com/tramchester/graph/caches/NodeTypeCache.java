package com.tramchester.graph.caches;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.NumberOfNodesAndRelationshipsRepository;
import com.tramchester.graph.graphbuild.GraphLabel;
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

import static com.tramchester.graph.graphbuild.GraphLabel.*;

@LazySingleton
public class NodeTypeCache implements NodeTypeRepository {
    private static final Logger logger = LoggerFactory.getLogger(NodeTypeCache.class);

    private final ConcurrentMap<GraphLabel, Set<Long>> labelMap;
    private final ConcurrentMap<Long, Boolean> queryNodes;

    private final Set<GraphLabel> nodesToCache = new HashSet<>(Arrays.asList(
            ROUTE_STATION, PLATFORM, SERVICE, HOUR, MINUTE, TRAM_STATION, BUS_STATION, TRAIN_STATION,
            FERRY_STATION, SUBWAY_STATION
    ));
    private final GraphDatabase graphDatabase;
    private final NumberOfNodesAndRelationshipsRepository numbersOfNodes;

    @Inject
    public NodeTypeCache(GraphDatabase graphDatabase, NumberOfNodesAndRelationshipsRepository numbersOfNodes,
                         StagedTransportGraphBuilder.Ready ready) {
        this.graphDatabase = graphDatabase;
        this.numbersOfNodes = numbersOfNodes;
        labelMap = new ConcurrentHashMap<>();

        for (GraphLabel label: nodesToCache) {
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
        labelMap.values().forEach(Set::clear);
        labelMap.clear();
    }

    private void populateCaches() {
        // populate
        try (TimedTransaction timed = new TimedTransaction(graphDatabase, logger, "populate node type cache")) {
            Transaction tx = timed.transaction();
            for (GraphLabel label : nodesToCache) {
                graphDatabase.findNodes(tx, label).stream().forEach(node -> put(node.getId(), label));
            }
        }

        // logging for diagnostics
        for (GraphLabel label : nodesToCache) {
            int size = labelMap.get(label).size();
            if (size>0) {
                logger.info("Loaded " + size + " for label " + label);
            } else {
                logger.info("Loaded zero nodes for label " + label);
            }
        }
    }


    private int getCapacity(GraphLabel label) {
        return numbersOfNodes.numberOf(label).intValue();
    }

    public void put(long id, GraphLabel label) {
        labelMap.get(label).add(id);
    }

    public void put(long id, Set<GraphLabel> labels) {
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

    private boolean has(final GraphLabel label, final long nodeId) {
        if (label == GraphLabel.QUERY_NODE) {
            return queryNodes.containsKey(nodeId);
        }
        return labelMap.get(label).contains(nodeId);
    }

    // for creating query nodes, to support MyLocation journeys
    @Override
    public Node createQueryNode(GraphDatabase graphDatabase, Transaction txn) {
        Node result = graphDatabase.createNode(txn, GraphLabel.QUERY_NODE);
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
