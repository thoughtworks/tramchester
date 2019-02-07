package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import org.neo4j.graphdb.Relationship;

public class MinuteRelationship extends TransportCostRelationship  {

    public MinuteRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Tram;
    }

    private MinuteRelationship(int cost, String id, TramNode startNode, TramNode endNode) {
        super(cost,id, startNode, endNode);
    }

    public static MinuteRelationship TestOnly(int cost, String id, TramNode startNode, TramNode endNode) {
        return new MinuteRelationship(cost,id,startNode,endNode);
    }

    @Override
    public boolean isMinuteLink() {
        return true;
    }

}
