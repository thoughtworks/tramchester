package com.tramchester.graph.search;

import com.tramchester.graph.GraphDatabase;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PopulateNodeIdsFromQuery {
    private final GraphDatabase graphDatabaseService;

    public PopulateNodeIdsFromQuery(GraphDatabase graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
    }

    @NotNull
    protected Set<Long> getNodeIdsForQuery(Map<String, Object> params, String query) {
        Set<Long> results;
        try (Transaction txn = graphDatabaseService.beginTx()) {
            Result result = txn.execute(query, params);
            ResourceIterator<Object> rows = result.columnAs("id");
            results = rows.stream().map(item -> (Long) item).collect(Collectors.toSet());
            result.close();
        }
        return results;
    }
}
