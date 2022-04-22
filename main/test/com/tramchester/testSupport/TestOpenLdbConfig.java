package com.tramchester.testSupport;

import com.tramchester.config.OpenLdbAppConfig;
import com.tramchester.config.OpenLdbConfig;

import java.net.URL;
import java.nio.file.Path;

public class TestOpenLdbConfig implements OpenLdbConfig {
    @Override
    public String getAccessToken() {
        return System.getenv("OPENLDB_APIKEY");
    }

    @Override
    public URL getWSDLLocation() {
        return OpenLdbAppConfig.formURL(Path.of("config", "OpenLDBWS.wsdl"));
    }
}
