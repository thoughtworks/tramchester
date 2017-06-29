package com.tramchester.integration.graph;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class CachingCostEvaluator implements CostEvaluator<Double>{
    Map<Long, Integer> idToCost;
    private String propertyName;

    public CachingCostEvaluator() {
        this.propertyName = GraphStaticKeys.COST;
        idToCost = new ConcurrentHashMap<>();
    }

    @Override
    public Double getCost(Relationship relationship, Direction direction) {
        long key = relationship.getId();
        if (!idToCost.containsKey(key)) {
            Integer value = (Integer) relationship.getProperty(propertyName);
            idToCost.put(key, value);
        }
        return idToCost.get(key).doubleValue();
    }
}
