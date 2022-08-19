package com.tramchester.dataimport.data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.mappers.serialisation.RouteIdDeserializer;
import com.tramchester.mappers.serialisation.RouteIdSerializer;

import java.util.Objects;

public class RouteIndexData {
    private Integer index;
    private IdFor<Route> routeId;

    private RouteIndexData() {
        // deserialization
    }

    public RouteIndexData(int index, IdFor<Route> routeId) {
        this.index = index;
        this.routeId = routeId;
    }


    public Integer getIndex() {
        return index;
    }

    @JsonSerialize(using = RouteIdSerializer.class)
    @JsonDeserialize(using = RouteIdDeserializer.class)
    public IdFor<Route> getRouteId() {
        return routeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteIndexData that = (RouteIndexData) o;
        return index.equals(that.index) && routeId.equals(that.routeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, routeId);
    }
}
