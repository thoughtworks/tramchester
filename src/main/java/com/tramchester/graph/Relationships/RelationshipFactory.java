package com.tramchester.graph.Relationships;

import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.Relationship;

public class RelationshipFactory {
//    Map<Long, TramRelationship> theCache;

//    public RelationshipFactory() {
//        theCache = new HashMap<>();
//    }

    public TramRelationship getRelationship(Relationship graphRelationship) {
//        long id = graphRelationship.getId();

//        if (theCache.containsKey(id)) {
//            return theCache.get(id);
//        }
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
        } else {
            throw new IllegalArgumentException("Unexpected relationship type: " + relationshipType);
        }
//        theCache.put(id,result);
        return result;
    }
}
