package com.tramchester.dataimport.data;

import com.tramchester.domain.Agency;
import com.tramchester.domain.GTFSTransportationType;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Route;

import java.util.Objects;

public class RouteData {

    private final IdFor<Route> id;
    private final String shortName;
    private final String longName;
    private final IdFor<Agency> agencyid;
    private final GTFSTransportationType routeType;

    public RouteData(String id, String agencyid, String shortName, String longName, GTFSTransportationType routeType) {
        this.id = IdFor.createId(id.intern());
        this.shortName = shortName;
        this.longName = longName;
        this.agencyid = IdFor.createId(agencyid);
        this.routeType = routeType;
    }

    public IdFor<Route> getId() {
        return id;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    public IdFor<Agency> getAgencyId() {
        return agencyid;
    }

    public GTFSTransportationType getRouteType() {
        return routeType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteData routeData = (RouteData) o;
        return Objects.equals(id, routeData.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
