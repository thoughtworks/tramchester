package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;


@LazySingleton
public
class NumberOfNodesRepository {
    private static final Logger logger = LoggerFactory.getLogger(NumberOfNodesRepository.class);

    private final Map<GraphBuilder.Labels, Long> counts;
    private final GraphDatabase graphDatabase;

    @Inject
    public NumberOfNodesRepository(GraphDatabase graphDatabase, StagedTransportGraphBuilder.Ready ready) {
        this.graphDatabase = graphDatabase;
        counts = new HashMap<>(GraphBuilder.Labels.values().length);
    }

    @PostConstruct
    void start() {
        logger.info("start");

        GraphBuilder.Labels[] labels = GraphBuilder.Labels.values();

        try (Transaction txn = graphDatabase.beginTx()) {
            for (GraphBuilder.Labels label : labels) {
                String name = label.name();
                long count = countNodesOfType(txn, name);
                counts.put(label, count);
                if (count>0) {
                    logger.info(count + " nodes of type " + name);
                }
            }
        }

        logger.info("statrted");
    }

    private long countNodesOfType(Transaction txn, String name) {
        String query = "MATCH (node:" + name + ") " + "RETURN count(*) as count";
        Result result = txn.execute(query);
        ResourceIterator<Object> rows = result.columnAs("count");
        long count = (long) rows.next();
        result.close();
        return count;
    }

    @PreDestroy
    void stop() {
        counts.clear();
    }

    public long numberOf(GraphBuilder.Labels label) {
        return counts.get(label);
    }
}
