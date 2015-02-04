package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.dropwizard.Configuration;


@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfiguration extends Configuration {

}
