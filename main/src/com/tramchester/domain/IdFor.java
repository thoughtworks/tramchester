package com.tramchester.domain;

import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.GraphStaticKeys.*;

public class IdFor<T> {
    private final String theId;

    protected IdFor(String theId) {
        this.theId = theId.intern();
    }

    public static <C extends HasId<C>> IdFor<C> createId(String id) {
        return new IdFor<>(id);
    }

    public static IdFor<RouteStation> createId(Station station, Route route) {
        String idAsString = station.getId().theId + route.getId().theId.replaceAll(" ", "");
        return createId(idAsString);
    }

    public static <CLASS> IdFor<CLASS> invalid() {
        return new IdFor<>("");
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdFor<?> idFor = (IdFor<?>) o;

        return theId.equals(idFor.theId);
    }

    @Override
    public String toString() {
        return "Id{'" + theId + "'}";
    }

    @Override
    public int hashCode() {
        return theId.hashCode();
    }

    public String forDTO() {
        return theId;
    }

    public String getGraphId() {
        return theId;
    }

    // TODO push Node names into Domain objects or GraphQuery?
    public boolean matchesStationNodePropery(Node node) {
        return node.getProperty(STATION_ID).toString().equals(theId);
    }

    public static IdFor<Route> getRouteIdFrom(Entity entity) {
        return new IdFor<>(entity.getProperty(ROUTE_ID).toString());
    }

    public static IdFor<Station> getStationIdFrom(Entity entity) {
        return new IdFor<>(entity.getProperty(STATION_ID).toString());
    }

    public static IdFor<Station> getTowardsStationIdFrom(Node endNode) {
        return new IdFor<>(endNode.getProperty(TOWARDS_STATION_ID).toString());
    }

    public static IdFor<Station> getIdFrom(Entity entity) {
        return new IdFor<>(entity.getProperty(ID).toString());
    }

    public static IdFor<Service> getServiceIdFrom(Node node) {
        return new IdFor<>(node.getProperty(SERVICE_ID).toString());
    }

    public static IdFor<Trip> getTripIdFrom(Entity entity) {
        return new IdFor<>(entity.getProperty(TRIP_ID).toString());
    }

    public static IdFor<RouteStation> getRouteStationIdFrom(Node node) {
        return new IdFor<>(node.getProperty(ROUTE_STATION_ID).toString());
    }

    public boolean notValid() {
        return theId.isEmpty();
    }
}
