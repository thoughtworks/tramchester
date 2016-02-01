package com.tramchester.graph.Relationships;


import com.tramchester.domain.TransportMode;
import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Relationship;

public abstract class TransportCostRelationship implements TransportRelationship {
    private int cost = -1;
    private String id = null;
    protected Relationship graphRelationship;

    public TransportCostRelationship(int cost, String id) {
        this.cost = cost;
        this.id = id;
    }

    public TransportCostRelationship(Relationship graphRelationship) {
        this.graphRelationship = graphRelationship;
    }

    @Override
    public int getCost() {
        if (cost==-1) {
            Object costProperty = graphRelationship.getProperty(GraphStaticKeys.COST);
            this.cost = Integer.parseInt(costProperty.toString());
        }
        return cost;
    }

    @Override
    public String getId() {
        if (id==null) {
            Object idProperty = graphRelationship.getProperty(GraphStaticKeys.ID);
            this.id = idProperty.toString();
        }
        return id;
    }

    @Override
    public abstract TransportMode getMode();

}
