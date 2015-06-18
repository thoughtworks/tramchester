package com.tramchester.domain;

public class Version {
    private String buildNumber;

    public Version(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getBuildNumber() {
        return buildNumber;
    }
}
