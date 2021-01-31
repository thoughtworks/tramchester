package com.tramchester.domain;

import com.tramchester.domain.reference.TransportMode;

import java.time.LocalDateTime;
import java.util.Set;

public class DataSourceInfo {

    private final DataSourceID sourceID;
    private final String version;
    private final LocalDateTime lastModTime;
    private final Set<TransportMode> modes;

    public DataSourceInfo(DataSourceID sourceID, String version, LocalDateTime lastModTime, Set<TransportMode> modes) {
        this.sourceID = sourceID;
        this.version = version;
        this.lastModTime = lastModTime;
        this.modes = modes;
    }

    public DataSourceID getID() {
        return sourceID;
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
                "name='" + sourceID + '\'' +
                ", version='" + version + '\'' +
                ", lastModTime=" + lastModTime +
                ", modes=" + modes +
                '}';
    }
}
