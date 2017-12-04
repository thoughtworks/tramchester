package com.tramchester.domain;

import static java.lang.String.format;

public class Platform {

    private String id;
    private String name;

    public String getPlatformNumber() {
        return platformNumber;
    }

    private String platformNumber;

    public Platform(String id, String name) {
        this.id = id;
        this.name = name;
        platformNumber = id.substring(id.length()-1);
    }


    public String getName() {
        return format("%s platform %s", name, platformNumber);
    }

    public String getId() {
        return id;
    }

}
