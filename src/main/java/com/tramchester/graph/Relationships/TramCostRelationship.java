package com.tramchester.graph.Relationships;


import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Relationship;

public abstract class TramCostRelationship implements TramRelationship {
    private final int cost;

    public TramCostRelationship(int cost) {
        this.cost = cost;
    }

    public TramCostRelationship(Relationship graphRelationship) {
        Object property = graphRelationship.getProperty(GraphStaticKeys.COST);
        cost = Integer.parseInt(property.toString());
    }

    @Override
    public int getCost() {
        return cost;    
    }

//    public Relationship getRelationship() {
//        return graphRelationship;
//    }
}
