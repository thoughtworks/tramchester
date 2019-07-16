package com.tramchester.domain.presentation;

public class Version {
    public static String MajorVersion = "2";

    private String versionNumber;

    public Version() {
        // deserialization
    }

    public Version(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    // from json
    public String getBuildNumber() {
        return versionNumber;
    }
}
