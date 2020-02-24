package com.tramchester.dataimport.data;

import java.util.Objects;

public class RouteData {
    private String id;
    private String shortName;
    private String longName;
    private String agency;

    public RouteData(String id, String agency, String shortName, String longName) {
        this.id = id.intern();
        this.shortName = shortName;
        this.longName = longName;
        this.agency = agency;
    }

    public String getId() {
        return id;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    public String getAgency() {
        return agency;
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
