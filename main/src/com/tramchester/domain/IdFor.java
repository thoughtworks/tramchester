package com.tramchester.domain;

import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphPropertyKeys;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Entity;

import static com.tramchester.graph.GraphPropertyKeys.*;

public class IdFor<T> implements Comparable<IdFor<T>> {
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

    public boolean notValid() {
        return theId.isEmpty();
    }

    public boolean isValid() {
        return !theId.isEmpty();
    }

    // TODO push Node names into Domain objects or GraphQuery?
    public boolean matchesStationNodePropery(Entity entity) {
        return getTheId(entity, STATION_ID).equals(theId);
    }

    public static IdFor<Route> getRouteIdFrom(Entity entity) {
        return new IdFor<>(getTheId(entity, ROUTE_ID));
    }

    public static IdFor<Station> getStationIdFrom(Entity entity) {
        return new IdFor<>(getTheId(entity, STATION_ID));
    }

    public static IdFor<Station> getTowardsStationIdFrom(Entity entity) {
        return new IdFor<>(getTheId(entity, TOWARDS_STATION_ID));
    }
    
    public static IdFor<Service> getServiceIdFrom(Entity entity) {
        return new IdFor<>(getTheId(entity, SERVICE_ID));
    }

    public static IdFor<Trip> getTripIdFrom(Entity entity) {
        return new IdFor<>(getTheId(entity, TRIP_ID));
    }

    public static IdFor<RouteStation> getRouteStationIdFrom(Entity entity) {
        return new IdFor<>(getTheId(entity, ROUTE_STATION_ID));
    }

    public static IdFor<Platform> getPlatformIdFrom(Entity entity) {
        return new IdFor<>(getTheId(entity, PLATFORM_ID));
    }

    private static String getTheId(Entity entity, GraphPropertyKeys propertyKey) {
        return entity.getProperty(propertyKey.getText()).toString();
    }

    @Override
    public int compareTo(@NotNull IdFor<T> other) {
        return theId.compareTo(other.theId);
    }
}
