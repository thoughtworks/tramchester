package com.tramchester.dataimport.data;

public class RouteData {
    private String id;
    private String code;
    private String name;
    private String agency;

    public RouteData(String id, String code, String name, String agency) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.agency = agency;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getAgency() {
        return agency;
    }


}
