package com.tramchester.domain.reference;

public enum RouteDirection {
        Inbound(":I:"),
        Outbound(":O:"),
        Circular(":C:"),
        Unknown("");

    private final String suffix;

    RouteDirection(String suffix) {
            this.suffix = suffix;
        }

    public String getSuffix() {
        return suffix;
    }
}

