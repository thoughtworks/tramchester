package com.tramchester.domain;

public class DataSourceID {
    private static final DataSourceID internal = new DataSourceID("internal");
    private static final DataSourceID tfgm = new DataSourceID("tfgm");
    private static final DataSourceID gbRail = new DataSourceID("gb-rail");

    private final String name;

    public DataSourceID(String name) {
        this.name = name;
    }

    public static DataSourceID Internal() {
        return internal;
    }

    public static DataSourceID TFGM() {
        return tfgm;
    }

    public static DataSourceID GBRail() {
        return gbRail;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "DataSourceName{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataSourceID that = (DataSourceID) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
