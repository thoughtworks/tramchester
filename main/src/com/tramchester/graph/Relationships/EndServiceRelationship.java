package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import org.neo4j.graphdb.Relationship;

public class EndServiceRelationship extends TransportCostRelationship  {

    public EndServiceRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Tram;
    }

    private EndServiceRelationship(int cost, String id, TramNode startNode, TramNode endNode) {
        super(cost,id, startNode, endNode);
    }

    public static EndServiceRelationship TestOnly(int cost, String id, TramNode startNode, TramNode endNode) {
        return new EndServiceRelationship(cost,id,startNode,endNode);
    }

    @Override
    public boolean isEndServiceLink() {
        return true;
    }

}
