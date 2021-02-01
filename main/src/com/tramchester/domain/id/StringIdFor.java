package com.tramchester.domain.id;

import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphPropertyKey;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Entity;

public class StringIdFor<T extends GraphProperty> implements Comparable<StringIdFor<T>>, IdFor<T> {
    private final String theId;

    private StringIdFor(String theId) {
        this.theId = theId;
    }

    public static <C extends HasId<C> & GraphProperty> StringIdFor<C> createId(String id) {
        return new StringIdFor<>(id);
    }

    public static <CLASS extends GraphProperty> StringIdFor<CLASS> invalid() {
        return new StringIdFor<>("");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StringIdFor<?> idFor = (StringIdFor<?>) o;

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

    @Override
    public String forDTO() {
        return theId;
    }

    @Override
    public String getGraphId() {
        return theId;
    }

    @Override
    public boolean notValid() {
        return theId.isEmpty();
    }

    @Override
    public boolean isValid() {
        return !theId.isEmpty();
    }

    public static <Z extends GraphProperty> StringIdFor<Z> getIdFromGraphEntity(Entity entity, GraphPropertyKey propertyKey) {
        String value =  entity.getProperty(propertyKey.getText()).toString();
        return new StringIdFor<>(value);
    }

    public static StringIdFor<RouteStation> createId(Station station, Route route) {
        // TODO remove replaceAll as route id now clear on initial import
        String idAsString = station.getId().theId + route.getId().theId.replaceAll(" ", "");
        return createId(idAsString);
    }

    public static StringIdFor<RouteStation> createId(StringIdFor<Station> station, StringIdFor<Route> route) {
        String idAsString = station.theId + route.theId;
        return createId(idAsString);
    }

    @Override
    public int compareTo(@NotNull StringIdFor<T> other) {
        return theId.compareTo(other.theId);
    }
}
