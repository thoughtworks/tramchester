package com.tramchester.domain;

import java.time.LocalDateTime;
import java.util.Set;

public class DataSourceInfo {

    private final String name;
    private final String version;
    private final LocalDateTime lastModTime;
    private final Set<TransportMode> modes;

    public DataSourceInfo(String name, String version, LocalDateTime lastModTime, Set<TransportMode> modes) {
        this.name = name;
        this.version = version;
        this.lastModTime = lastModTime;
        this.modes = modes;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public LocalDateTime getLastModTime() {
        return lastModTime;
    }

    public Set<TransportMode> getModes() {
        return modes;
    }

    @Override
    public String toString() {
        return "DataSourceInfo{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", lastModTime=" + lastModTime +
                ", modes=" + modes +
                '}';
    }
}
