package com.tramchester.domain;

import com.tramchester.domain.id.IdFor;
import com.tramchester.graph.GraphPropertyKey;

import java.util.*;

public class Agency implements HasId<Agency>, GraphProperty {
    private final Set<Route> routes;
    private final IdFor<Agency> agencyId;
    private final String agencyName;
    private final DataSourceID dataSourceID;

    public static final Agency Walking;
    private static final Agency Metrolink;

    static {
        Walking = new Agency(DataSourceID.Internal(), "Walking", "Walking");
        Metrolink = new Agency(DataSourceID.TFGM(), "MET", "Metrolink");
    }

    public Agency(DataSourceID dataSourceID, String agencyId, String agencyName) {
        this.dataSourceID = dataSourceID;
        this.agencyId =  IdFor.createId(agencyId);
        this.agencyName = agencyName;
        routes = new HashSet<>();
    }

    public static boolean IsMetrolink(IdFor<Agency> agencyId) {
        return Metrolink.getId().equals(agencyId);
    }

    public void addRoute(Route route) {
        routes.add(route);
    }

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
        Agency agency = (Agency) o;
        return agencyId.equals(agency.agencyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agencyId);
    }

    public IdFor<Agency> getId() {
        return agencyId;
    }

    public String getName() {
        return agencyName;
    }

    @Override
    public GraphPropertyKey getProp() {
        throw new RuntimeException("No ID property for agency");
    }

    public DataSourceID getDataSourceID() {
        return dataSourceID;
    }
}
