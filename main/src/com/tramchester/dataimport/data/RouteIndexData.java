package com.tramchester.dataimport.data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.mappers.serialisation.RouteIdDeserializer;
import com.tramchester.mappers.serialisation.RouteIdSerializer;

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
}
