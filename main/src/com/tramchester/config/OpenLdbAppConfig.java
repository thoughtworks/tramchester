package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

public class OpenLdbAppConfig implements OpenLdbConfig {

    private final String accessToken;
    private final Path wsdlLocation;

    public OpenLdbAppConfig(@JsonProperty(value ="accessToken", required = true) String accessToken,
            @JsonProperty(value="wsdlLocation", required = true) Path wsdlLocation) {
        this.accessToken = accessToken;
        this.wsdlLocation = wsdlLocation;
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public URL getWSDLLocation() {
        return formURL(wsdlLocation);
    }

    public static URL formURL(Path wsdlLocation) {
        Path location = wsdlLocation.toAbsolutePath();
        URI uri = location.toUri();
        try {
            return uri.toURL();
        } catch (MalformedURLException urlException) {
            throw new RuntimeException("Bad value for wsdlLocation " + uri, urlException);
        }
    }
}
