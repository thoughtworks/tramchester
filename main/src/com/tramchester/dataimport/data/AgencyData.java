package com.tramchester.dataimport.data;

public class AgencyData {
    private final String id;
    private final String name;

    public AgencyData(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}
