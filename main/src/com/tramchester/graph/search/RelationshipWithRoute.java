package com.tramchester.graph.search;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.graph.graphbuild.GraphProps;
import org.neo4j.graphdb.Relationship;

public class RelationshipWithRoute implements HasId<Route> {
    private final Relationship relationship;
    private final IdFor<Route> routeId;

    public RelationshipWithRoute(Relationship relationship) {
        routeId = GraphProps.getRouteIdFrom(relationship);
        this.relationship = relationship;
    }

    public Relationship getRelationship() {
        return relationship;
    }

    @Override
    public IdFor<Route> getId() {
        return routeId;
    }
}
