package com.tramchester.graph.databaseManagement;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@LazySingleton
public class GraphDatabaseMetaInfo {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabaseMetaInfo.class);

    public boolean isNeighboursEnabled(Transaction txn) {
        return hasAnyNodeWith(txn, GraphLabel.NEIGHBOURS_ENABLED);
    }

    public boolean hasVersionInfo(Transaction txn) {
        return hasAnyNodeWith(txn, GraphLabel.VERSION);
    }

    private boolean hasAnyNodeWith(Transaction txn, GraphLabel label) {
        ResourceIterator<Node> query = txn.findNodes(label);
        return query.stream().findAny().isPresent();
    }

    public void setNeighboursEnabled(Transaction txn) {
        txn.createNode(GraphLabel.NEIGHBOURS_ENABLED);
    }

    public Map<String, String> getVersions(Transaction txn) {
        ResourceIterator<Node> query = txn.findNodes(GraphLabel.VERSION);

        Map<String, String> versions = new HashMap<>();
        query.stream().forEach(versionNode -> {
            final Map<String, Object> nodeProps = versionNode.getAllProperties();
            logger.info("Got properties for VERSION node " + nodeProps.toString());
            nodeProps.forEach((key, value) -> versions.put(key, value.toString()));
        });

        return versions;
    }

    public void createVersionNode(Transaction tx, Set<DataSourceInfo> infos) {
        Node node = tx.createNode(GraphLabel.VERSION);
        infos.forEach(nameAndVersion -> GraphProps.setProp(node, nameAndVersion));
    }
}
