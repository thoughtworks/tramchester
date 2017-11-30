package com.tramchester.graph.Relationships;

import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.Relationship;

public class RelationshipFactory {

    private NodeFactory nodeFactory;

    public RelationshipFactory(NodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    public TransportRelationship getRelationship(Relationship graphRelationship) {
        String name = graphRelationship.getType().name();
        TransportRelationshipTypes relationshipType = TransportRelationshipTypes.valueOf(name);
        switch (relationshipType) {
            case TRAM_GOES_TO: return new TramGoesToRelationship(graphRelationship, nodeFactory);
            case BUS_GOES_TO: return new BusGoesToRelationship(graphRelationship, nodeFactory);
            case DEPART: return new DepartRelationship(graphRelationship, nodeFactory);
            case BOARD: return new BoardRelationship(graphRelationship, nodeFactory);
            case INTERCHANGE_DEPART: return new InterchangeDepartsRelationship(graphRelationship, nodeFactory);
            case INTERCHANGE_BOARD: return new InterchangeBoardsRelationship(graphRelationship, nodeFactory);
            case WALKS_TO: return new WalksToRelationship(graphRelationship, nodeFactory);
            case ENTER_PLATFORM: return new EnterPlatformRelationship(graphRelationship, nodeFactory);
            case LEAVE_PLATFORM: return new LeavePlatformRelationship(graphRelationship, nodeFactory);
            default:
                throw new IllegalArgumentException("Unexpected relationship type: " + relationshipType);
        }
    }
}
