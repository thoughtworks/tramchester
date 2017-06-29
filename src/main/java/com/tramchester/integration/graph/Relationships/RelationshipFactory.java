package com.tramchester.integration.graph.Relationships;

import com.tramchester.integration.graph.Nodes.NodeFactory;
import com.tramchester.integration.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.Relationship;

public class RelationshipFactory {

    private NodeFactory nodeFactory;

    public RelationshipFactory(NodeFactory nodeFactory) {

        this.nodeFactory = nodeFactory;
    }

    public TransportRelationship getRelationship(Relationship graphRelationship) {

        TransportRelationship result;
        String relationshipType = graphRelationship.getType().name();
        if (relationshipType.equals(TransportRelationshipTypes.TRAM_GOES_TO.toString())) {
            result = new TramGoesToRelationship(graphRelationship, nodeFactory);
        } else if (relationshipType.equals(TransportRelationshipTypes.BUS_GOES_TO.toString())) {
                result = new BusGoesToRelationship(graphRelationship, nodeFactory);
        } else if (relationshipType.equals(TransportRelationshipTypes.DEPART.toString())) {
            result = new DepartRelationship(graphRelationship, nodeFactory);
        }  else if (relationshipType.equals(TransportRelationshipTypes.BOARD.toString())) {
            result = new BoardRelationship(graphRelationship, nodeFactory);
        } else if (relationshipType.equals(TransportRelationshipTypes.INTERCHANGE_DEPART.toString())) {
            result = new InterchangeDepartsRelationship(graphRelationship, nodeFactory);
        } else if (relationshipType.equals(TransportRelationshipTypes.INTERCHANGE_BOARD.toString())) {
            result = new InterchangeBoardsRelationship(graphRelationship, nodeFactory);
        } else if (relationshipType.endsWith(TransportRelationshipTypes.WALKS_TO.toString())) {
            result = new WalksToRelationship(graphRelationship, nodeFactory);
        }
        else {
            throw new IllegalArgumentException("Unexpected relationship type: " + relationshipType);
        }
        return result;
    }
}
