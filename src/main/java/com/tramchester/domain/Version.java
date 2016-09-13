package com.tramchester.domain;

public class Version {
    private String buildNumber;

    public Version() {
        // deserialization
    }

    public Version(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    // from json
    public String getBuildNumber() {
        return buildNumber;
    }
}
