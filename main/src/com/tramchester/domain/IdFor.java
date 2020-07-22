package com.tramchester.domain;

import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;

import static com.tramchester.graph.GraphStaticKeys.STATION_ID;

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
        return "IdFor{" +
                "theId='" + theId + '\'' +
                '}';
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

    @NotNull
    public static <C extends HasId<C>> IdFor<C> getIdFrom(Entity entity, String propertyName) {
        return new IdFor<>(entity.getProperty(propertyName).toString());
    }

    public boolean notValid() {
        return theId.isEmpty();
    }
}
