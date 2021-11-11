package com.tramchester.graph.search;

import com.tramchester.domain.RouteReadOnly;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphProps;
import org.neo4j.graphdb.Relationship;

public class RelationshipWithRoute implements HasId<RouteReadOnly> {
    private final Relationship relationship;
    private final IdFor<RouteReadOnly> routeId;

    public RelationshipWithRoute(Relationship relationship) {
        routeId = GraphProps.getRouteIdFrom(relationship);
        this.relationship = relationship;
    }

    public IdFor<RouteReadOnly> getRouteId() {
        return routeId;
    }

    public Relationship getRelationship() {
        return relationship;
    }

    @Override
    public GraphPropertyKey getProp() {
        return null;
    }

    @Override
    public IdFor<RouteReadOnly> getId() {
        return routeId;
    }
}
