package com.tramchester.integration.testSupport;


import com.tramchester.config.AppConfiguration;
import io.dropwizard.Application;
import io.dropwizard.testing.junit5.DropwizardAppExtension;

public class IntegrationAppExtension extends DropwizardAppExtension<AppConfiguration> {

    public IntegrationAppExtension(Class<? extends Application<AppConfiguration>> applicationClass, AppConfiguration configuration) {
        super(applicationClass, configuration);
    }
}
