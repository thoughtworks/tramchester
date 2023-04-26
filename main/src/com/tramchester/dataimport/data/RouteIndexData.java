package com.tramchester.dataimport.data;

import com.tramchester.caching.CachableData;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;

import java.util.Objects;

public class RouteIndexData implements CachableData {
    private short index;
    private IdFor<Route> routeId;

    private RouteIndexData() {
        // deserialization
    }

    public RouteIndexData(short index, IdFor<Route> routeId) {
        this.index = index;
        this.routeId = routeId;
    }


    public short getIndex() {
        return index;
    }

    public IdFor<Route> getRouteId() {
        return routeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteIndexData that = (RouteIndexData) o;
        return index == that.index && routeId.equals(that.routeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, routeId);
    }
}
