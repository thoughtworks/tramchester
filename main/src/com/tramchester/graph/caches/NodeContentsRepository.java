package com.tramchester.graph.caches;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.CachedNodeOperations;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

@ImplementedBy(CachedNodeOperations.class)
public interface NodeContentsRepository  {

    IdFor<Service> getServiceId(Node node);
    TramTime getTime(Node node);
    int getHour(Node node);

    IdFor<Trip> getTrip(Relationship relationship);

    int getCost(Relationship lastRelationship);
    void deleteFromCostCache(Relationship relationship);
}
