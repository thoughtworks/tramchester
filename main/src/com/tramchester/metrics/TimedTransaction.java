package com.tramchester.metrics;

import com.tramchester.graph.GraphDatabase;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;

public class TimedTransaction implements AutoCloseable{
    private final Transaction transaction;
    private final Timing timing;

    public TimedTransaction(GraphDatabase databaseService, Logger logger, String name) {
        this.transaction = databaseService.beginTx();
        timing = new Timing(logger, "transcation " + name);
    }

    @Override
    public void close() {
        transaction.close();
        timing.close();
    }

    public Transaction transaction() {
        return transaction;
    }
}
