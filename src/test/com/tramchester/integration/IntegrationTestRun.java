package com.tramchester.integration;


import com.tramchester.config.AppConfiguration;
import io.dropwizard.Application;
import io.dropwizard.testing.junit.DropwizardAppRule;

public class IntegrationTestRun extends DropwizardAppRule<AppConfiguration> {

    public IntegrationTestRun(Class<? extends Application<AppConfiguration>> applicationClass, AppConfiguration configuration) {
        super(applicationClass, configuration);
    }
}
