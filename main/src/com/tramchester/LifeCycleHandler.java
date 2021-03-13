package com.tramchester;

import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;


public class LifeCycleHandler implements LifeCycle.Listener {
    private static final Logger logger = LoggerFactory.getLogger(LifeCycleHandler.class);

    private final GuiceContainerDependencies dependencies;
    private final ScheduledExecutorService executor;

    public LifeCycleHandler(GuiceContainerDependencies dependencies, ScheduledExecutorService executor) {
        this.dependencies = dependencies;
        this.executor = executor;
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
        logger.info("Shutdown dependencies");
        dependencies.close();
        logger.info("Stop scheduled tasks");
        executor.shutdown();
        logger.info("Dependencies stopped");
    }

    @Override
    public void lifeCycleStopped(LifeCycle event) {
        logger.info("Dropwizard stoppped");
    }
}
