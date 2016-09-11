package com.tramchester;

import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LifeCycleHandler implements LifeCycle.Listener {
    private static final Logger logger = LoggerFactory.getLogger(Dependencies.class);

    private Dependencies dependencies;

    public LifeCycleHandler(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public void lifeCycleStarting(LifeCycle event) {
        logger.info("Dropwizard starting");
    }

    @Override
    public void lifeCycleStarted(LifeCycle event) {
        logger.info("Dropwizard started");
    }

    @Override
    public void lifeCycleFailure(LifeCycle event, Throwable cause) {
        logger.error("Dropwizard failure", cause);
    }

    @Override
    public void lifeCycleStopping(LifeCycle event) {
        logger.info("Dropwizard stopping");
        dependencies.close();
        logger.info("Dependencies stopped");
    }

    @Override
    public void lifeCycleStopped(LifeCycle event) {
        logger.info("Dropwizard stoppped");
    }
}
