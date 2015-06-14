package com.tramchester.graph.Relationships;

import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.Relationship;

public class RelationshipFactory {
    public TramRelationship getRelationship(Relationship graphRelationship) {
        String relationshipType = graphRelationship.getType().name();
        if (relationshipType.equals(TransportRelationshipTypes.GOES_TO.toString())) {
            return new GoesToRelationship(graphRelationship);
        } else if (relationshipType.equals(TransportRelationshipTypes.DEPART.toString())) {
            return new DepartRelationship(graphRelationship);
        }  else if (relationshipType.equals(TransportRelationshipTypes.BOARD.toString())) {
            return new BoardRelationship(graphRelationship);
        } else if (relationshipType.equals(TransportRelationshipTypes.INTERCHANGE_DEPART.toString())) {
            return new InterchangeDepartsRelationship(graphRelationship);
        } else if (relationshipType.equals(TransportRelationshipTypes.INTERCHANGE_BOARD.toString())) {
            return new InterchangeBoardsRelationship(graphRelationship);
        } else {
            throw new IllegalArgumentException("Unexpected relationship type: " + relationshipType);
        }
    }
}
