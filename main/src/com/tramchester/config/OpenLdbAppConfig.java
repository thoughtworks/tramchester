package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenLdbAppConfig implements OpenLdbConfig {

    private final String accessToken;

    public OpenLdbAppConfig(@JsonProperty(value ="accessToken", required = true) String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }
}
