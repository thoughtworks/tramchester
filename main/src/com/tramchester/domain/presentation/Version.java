package com.tramchester.domain.presentation;

public class Version {
    public static final String MajorVersion = "2";

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
