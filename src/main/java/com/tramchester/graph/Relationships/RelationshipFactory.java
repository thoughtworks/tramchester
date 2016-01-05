package com.tramchester.graph.Relationships;

import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.Relationship;

public class RelationshipFactory {

    public TramRelationship getRelationship(Relationship graphRelationship) {

        TramRelationship result;
        String relationshipType = graphRelationship.getType().name();
        if (relationshipType.equals(TransportRelationshipTypes.TRAM_GOES_TO.toString())) {
            result = new TramGoesToRelationship(graphRelationship);
        } else if (relationshipType.equals(TransportRelationshipTypes.DEPART.toString())) {
            result = new DepartRelationship(graphRelationship);
        }  else if (relationshipType.equals(TransportRelationshipTypes.BOARD.toString())) {
            result = new BoardRelationship(graphRelationship);
        } else if (relationshipType.equals(TransportRelationshipTypes.INTERCHANGE_DEPART.toString())) {
            result = new InterchangeDepartsRelationship(graphRelationship);
        } else if (relationshipType.equals(TransportRelationshipTypes.INTERCHANGE_BOARD.toString())) {
            result = new InterchangeBoardsRelationship(graphRelationship);
        } else if (relationshipType.endsWith(TransportRelationshipTypes.WALKS_TO.toString())) {
            result = new WalksToRelationship();
        }
        else {
            throw new IllegalArgumentException("Unexpected relationship type: " + relationshipType);
        }
        return result;
    }
}
