package com.tramchester.graph.graphbuild;

import com.tramchester.domain.DataSourceInfo;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ValidateGraphFeedInfoVersion implements Startable {
    private static final Logger logger = LoggerFactory.getLogger(ValidateGraphFeedInfoVersion.class);

    private final GraphDatabase graphDatabase;
    private final TransportData transportData;

    public ValidateGraphFeedInfoVersion(GraphDatabase graphDatabase, TransportData transportData) {
        this.graphDatabase = graphDatabase;
        this.transportData = transportData;
    }

    @Override
    public void start() {
        DataSourceInfo info = transportData.getDataSourceInfo();
        logger.info("Checking graph version information ");

        try(Transaction transaction = graphDatabase.beginTx()) {

            ResourceIterator<Node> query = graphDatabase.findNodes(transaction, GraphBuilder.Labels.VERSION);
            List<Node> nodes = query.stream().collect(Collectors.toList());

            if (nodes.size()>1) {
                logger.error("Too many VERSION nodes, will use first");
            }

            if (nodes.isEmpty()) {
                String msg = "Missing VERSION node, cannot check versions";
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            Node versionNode = nodes.get(0);
            Map<String, Object> allProps = versionNode.getAllProperties();

            info.getVersions().forEach(nameAndVersion -> {
                String name = nameAndVersion.getName();
                logger.info("Checking version for " + name);

                if (allProps.containsKey(name)) {
                    String graphValue = allProps.get(name).toString();
                    if (!nameAndVersion.getVersion().equals(graphValue)) {
                        logger.error(format("Mismatch on graph VERSION, got '%s' for %s", graphValue, nameAndVersion));
                    } else {
                        logger.info("Got correct VERSION node value for " + nameAndVersion);
                    }
                } else {
                    logger.error("Could not find version for " + name + " properties were " + allProps.toString());
                }

            });
        }
    }

    @Override
    public void stop() {
        // no op
    }
}
