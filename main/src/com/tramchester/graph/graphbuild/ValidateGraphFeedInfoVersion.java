package com.tramchester.graph.graphbuild;

import com.tramchester.graph.GraphDatabase;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static com.tramchester.graph.GraphStaticKeys.TIME;
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
        String dataVersion = transportData.getVersion();

        logger.info("Checking graph version against " + dataVersion);

        try(Transaction transaction = graphDatabase.beginTx()) {
            ResourceIterator<Node> query = graphDatabase.findNodes(transaction, GraphBuilder.Labels.VERSION);
            List<Node> nodes = query.stream().collect(Collectors.toList());

            if (nodes.isEmpty()) {
                logger.error("Missing VERSION node, cannot check versions");
            }
            if (nodes.size()>1) {
                logger.error("Too many VERSION nodes");
            }

            Node expected = nodes.get(0);
            String graphValue = expected.getProperty(TIME).toString();
            if (!dataVersion.equals(graphValue)) {
                logger.error(format("Mismatch on graph VERSION, got '%s' expected '%s'", graphValue, dataVersion));
            } else {
                logger.info("Got correct VERSION node value of " + graphValue);
            }

        }
    }

    @Override
    public void stop() {
        // no op
    }
}
