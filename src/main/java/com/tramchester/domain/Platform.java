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

    @Override
    public String toString() {
        return "Platform{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", platformNumber='" + platformNumber + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Platform platform = (Platform) o;

        return id.equals(platform.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
