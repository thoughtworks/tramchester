package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.GraphStaticKeys.SERVICE_ID;

public class HourRelationship extends TransportCostRelationship  {

    public HourRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Tram;
    }

    private HourRelationship(int cost, String id, TramNode startNode, TramNode endNode) {
        super(cost,id, startNode, endNode);
    }

    public static HourRelationship TestOnly(int cost, String id, TramNode startNode, TramNode endNode) {
        return new HourRelationship(cost,id,startNode,endNode);
    }

    @Override
    public boolean isServiceLink() { return true; }

    public String getServiceId() {
        return  graphRelationship.getProperty(SERVICE_ID).toString();
    }

}
