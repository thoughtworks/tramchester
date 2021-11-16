package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.graph.GraphPropertyKey;

import java.util.*;

public class MutableAgency implements Agency {
    private final Set<Route> routes;
    private final IdFor<Agency> agencyId;
    private final String agencyName;
    private final DataSourceID dataSourceID;

    public static final Agency Walking;
    public static final IdFor<Agency> METL;

    static {
        Walking = new MutableAgency(DataSourceID.internal, StringIdFor.createId("Walking"), "Walking");
        METL = StringIdFor.createId("METL");
    }

    public MutableAgency(DataSourceID dataSourceID, IdFor<Agency> agencyId, String agencyName) {
        this.dataSourceID = dataSourceID;
        this.agencyId =  agencyId;
        this.agencyName = agencyName;
        routes = new HashSet<>();
    }

    // test support
    public static Agency build(DataSourceID dataSourceID, IdFor<Agency> agencyId, String agencyName) {
        return new MutableAgency(dataSourceID, agencyId, agencyName);
    }

    public void addRoute(Route route) {
        routes.add(route);
    }

    @Override
    public Collection<Route> getRoutes() {
        return routes;
    }

    @Override
    public String toString() {
        return "Agency{" +
                "routes=" + HasId.asIds(routes) +
                ", agencyId=" + agencyId +
                ", agencyName='" + agencyName + '\'' +
                ", dataSourceID=" + dataSourceID +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutableAgency agency = (MutableAgency) o;
        return agencyId.equals(agency.agencyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agencyId);
    }

    @Override
    public IdFor<Agency> getId() {
        return agencyId;
    }

    @Override
    public String getName() {
        return agencyName;
    }

    @Override
    public GraphPropertyKey getProp() {
        throw new RuntimeException("No ID property for agency");
    }

}
