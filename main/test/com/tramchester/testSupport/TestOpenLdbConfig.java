package com.tramchester.testSupport;

import com.tramchester.config.OpenLdbConfig;

public class TestOpenLdbConfig implements OpenLdbConfig {
    @Override
    public String getAccessToken() {
        return System.getenv("OPENLDB_APIKEY");
    }
}
