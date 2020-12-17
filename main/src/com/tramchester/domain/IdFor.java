package com.tramchester.domain;

import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphPropertyKey;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Entity;

import static com.tramchester.graph.GraphPropertyKey.*;

public class IdFor<T extends GraphProperty> implements Comparable<IdFor<T>> {
    private final String theId;

    private IdFor(String theId) {
        this.theId = theId;
    }

    public static <C extends HasId<C> & GraphProperty> IdFor<C> createId(String id) {
        return new IdFor<>(id);
    }

    public static <CLASS extends GraphProperty> IdFor<CLASS> invalid() {
        return new IdFor<>("");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdFor<?> idFor = (IdFor<?>) o;

        // interned strings, so == is ok here
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

    public static IdFor<RouteStation> createId(Station station, Route route) {
        // TODO remove replaceAll as route id now clear on initial import
        String idAsString = station.getId().theId + route.getId().theId.replaceAll(" ", "");
        return createId(idAsString);
    }

    public static IdFor<RouteStation> createId(IdFor<Station> station, IdFor<Route> route) {
        String idAsString = station.theId + route.theId;
        return createId(idAsString);
    }

    private static String getTheId(Entity entity, GraphPropertyKey propertyKey) {
        return entity.getProperty(propertyKey.getText()).toString();
    }

    @Override
    public int compareTo(@NotNull IdFor<T> other) {
        return theId.compareTo(other.theId);
    }
}
