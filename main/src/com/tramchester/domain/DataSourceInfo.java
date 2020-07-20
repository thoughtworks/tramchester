package com.tramchester.domain;

import java.util.Set;

public class DataSourceInfo {
    private final Set<NameAndVersion> nameAndVersions;

    public DataSourceInfo(Set<NameAndVersion> nameAndVersions) {
        this.nameAndVersions = nameAndVersions;
    }

    public Set<NameAndVersion> getVersions() {
        return nameAndVersions;
    }

    @Override
    public String toString() {
        return "DataSourceInfo{" +
                "nameAndVersions=" + nameAndVersions +
                '}';
    }

    public static class NameAndVersion {
        @Override
        public String toString() {
            return "NameAndVersion{" +
                    "name='" + name + '\'' +
                    ", version=" + version +
                    '}';
        }

        private final String name;
        private final String version;

        public NameAndVersion(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }
    }
}
