package com.tramchester.domain;

import static java.lang.String.format;

public class Version {
    public static String MajorVersion = "1";

    private String buildNumber;

    public Version() {
        // deserialization
    }

    public Version(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    // from json
    public String getBuildNumber() {
        return format("%s.%s", MajorVersion, buildNumber);
    }
}
