package com.tramchester.graph.Relationships;


import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Relationship;

public abstract class TramCostRelationship implements TramRelationship {
    private final Relationship graphRelationship;
    protected int cost;

    public TramCostRelationship(Relationship graphRelationship) {
        this.graphRelationship = graphRelationship;
        this.cost = Integer.parseInt(graphRelationship.getProperty(GraphStaticKeys.COST).toString());
    }

    @Override
    public int getCost() {
        return cost;
    }

    public Relationship getRelationship() {
        return graphRelationship;
    }
}
