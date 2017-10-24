package com.tramchester.graph.Relationships;


import com.tramchester.domain.TransportMode;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import org.neo4j.graphdb.Relationship;

public abstract class TransportCostRelationship implements TransportRelationship {
    protected Relationship graphRelationship;
    private NodeFactory nodeFactory;

    private int cost = -1;
    private String id = null;
    private TramNode startNode;
    private TramNode endNode;

    protected TransportCostRelationship(int cost, String id, TramNode startNode, TramNode endNode) {
        // testing only
        this.cost = cost;
        this.id = id;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public TransportCostRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        this.graphRelationship = graphRelationship;
        this.nodeFactory = nodeFactory;
    }

    @Override
    public int getCost() {
        if (cost==-1) {
            Object costProperty = graphRelationship.getProperty(GraphStaticKeys.COST);
            cost = Integer.parseInt(costProperty.toString());
        }
        return cost;
    }

    @Override
    public String getId() {
        if (id==null) {
            Object idProperty = graphRelationship.getProperty(GraphStaticKeys.ID);
            id = idProperty.toString();
        }
        return id;
    }

    @Override
    public abstract TransportMode getMode();

    @Override
    public TramNode getStartNode() {
        if (startNode==null) {
            startNode = nodeFactory.getNode(graphRelationship.getStartNode());
        }
        return startNode;
    }

    @Override
    public TramNode getEndNode() {
        if (endNode==null) {
            endNode = nodeFactory.getNode(graphRelationship.getEndNode());
        }
        return endNode;
    }

}
