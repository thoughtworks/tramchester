package com.tramchester.domain.id;

import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;

public class RouteStationId implements IdFor<RouteStation> {

    private static final String DIVIDER = "_";

    private final IdFor<Route> routeId;
    private final IdFor<Station> stationId;

    private RouteStationId(final IdFor<Route> routeId, final IdFor<Station> stationId) {
        this.routeId = routeId;
        this.stationId = stationId;
    }

    public static IdFor<RouteStation> createId(final IdFor<Route> routeId, final IdFor<Station> stationId) {
        return new RouteStationId(routeId, stationId);
    }

    public static IdFor<RouteStation> parse(final String text) {
        final int indexOf = text.indexOf(DIVIDER);
        if (indexOf<0) {
            return StringIdFor.invalid(RouteStation.class);
        }
        // todo rail route or normal route id?
        final IdFor<Route> routeId  = Route.createId(text.substring(0, indexOf));
        final IdFor<Station> stationId = Station.createId(text.substring(indexOf+1));
        return createId(routeId, stationId);
    }

    @Override
    public String getGraphId() {
        return routeId.getGraphId()+DIVIDER+stationId.getGraphId();
    }

    @Override
    public boolean isValid() {
        return routeId.isValid() && stationId.isValid();
    }

    @Override
    public Class<RouteStation> getDomainType() {
        return RouteStation.class;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RouteStationId that = (RouteStationId) o;

        if (!routeId.equals(that.routeId)) return false;
        return stationId.equals(that.stationId);
    }

    @Override
    public int hashCode() {
        int result = routeId.hashCode();
        result = 31 * result + stationId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RouteStationId{" +
                "routeId=" + routeId +
                ", stationId=" + stationId +
                '}';
    }
}
