package com.tramchester.graph;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.neo4j.graphdb.Path;

public interface BasicServiceHeuristics {
    ServiceReason checkServiceHeuristics(TransportRelationship incoming, GoesToRelationship goesToRelationship, Path path) throws TramchesterException;
}
