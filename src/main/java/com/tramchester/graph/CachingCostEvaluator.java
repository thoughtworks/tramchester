package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachingCostEvaluator implements CostEvaluator<Double>{
    private static final Logger logger = LoggerFactory.getLogger(CachingCostEvaluator.class);

    private final boolean cacheDisabled;
    Map<Long, Double> idToCost;
    private String propertyName;

    public CachingCostEvaluator(TramchesterConfig config) {
        this.propertyName = GraphStaticKeys.COST;
        idToCost = new ConcurrentHashMap<>();
        cacheDisabled = config.getEdgePerTrip();
        if (cacheDisabled) {
            logger.warn("Caching is disabled, EdgePerTrip is on");
        }
    }

    @Override
    public Double getCost(Relationship relationship, Direction direction) {
        if (cacheDisabled) {
            Integer amount = (Integer) relationship.getProperty(propertyName);
            return amount.doubleValue();
        }


        long key = relationship.getId();
        if (idToCost.containsKey(key)) {
            return idToCost.get(key);
        }
        Integer amount = (Integer) relationship.getProperty(propertyName);
        Double value = amount.doubleValue();
        idToCost.put(key, value);
        return value;
    }
}
