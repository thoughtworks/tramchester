package com.tramchester;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.tramchester.healthchecks.RegistersHealthchecks;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.resources.APIResource;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GuiceContainerDependencies implements ComponentContainer {
    private static final Logger logger = LoggerFactory.getLogger(GuiceContainerDependencies.class);

    private final Reflections reflections;
    private final Injector injector;

    public GuiceContainerDependencies(List<AbstractModule> moduleList) {
        reflections = new Reflections(App.class.getPackageName());
        injector = LifecycleInjector.builder().
                withModules(moduleList).build().
                createInjector();
    }

    public void initialise() {
        logger.info("initialise");

        if (logger.isDebugEnabled()) {
            logger.warn("Debug logging is enabled, server performance will be impacted");
        }

        logger.info("Start components");
        LifecycleManager manager = injector.getInstance(LifecycleManager.class);
        try {
            manager.start();
        } catch (Exception e) {
            logger.error("Failed to start", e);
            throw new RuntimeException("Failed to start", e);
        }

        if (manager.hasStarted()) {
            logger.info("Lifecycle manager has started");
        } else {
            logger.error("Lifecycle manager not started");
        }

        logger.info("Done");
    }

    public Set<Class<? extends APIResource>> getResources() {
        Set<Class<? extends APIResource>> apiResources = reflections.getSubTypesOf(APIResource.class);
        Set<Class<?>> havePath = reflections.getTypesAnnotatedWith(Path.class);

        Set<Class<? extends APIResource>> pathMissing = apiResources.stream().
                filter(apiType -> !havePath.contains(apiType)).collect(Collectors.toSet());
        if (!pathMissing.isEmpty()) {
            final String msg = "The following API resources lack a path: " + pathMissing;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        Set<Class<?>> pathNotAPIResource = havePath.stream().
                filter(hasPathType -> !apiResources.contains(hasPathType)).collect(Collectors.toSet());
        if (!pathNotAPIResource.isEmpty()) {
            final String msg = "The following Path annotated classes don't implement APIResource: " + pathNotAPIResource;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        return apiResources;
    }

    public void registerHealthchecksInto(HealthCheckRegistry healthChecks) {
        RegistersHealthchecks instance = get(RegistersHealthchecks.class);
        instance.registerAllInto(healthChecks);
    }

    public void close() {
        logger.info("Dependencies close");

        logger.info("Begin cache stats");
        CacheMetrics cacheMetrics = get(CacheMetrics.class);
        cacheMetrics.report();
        logger.info("End cache stats");

        logger.info("Stop components");
        stop();

        logger.info("Dependencies closed");
        System.gc(); // for tests which accumulate/free a lot of memory
    }

    protected void stop() {
        LifecycleManager manager = injector.getInstance(LifecycleManager.class);
        if (manager==null) {
            logger.error("Unable to get lifecycle manager for close()");
        } else {
            logger.info("Services Manager close");
            manager.close();
        }
    }

    public <C> C get(Class<C> klass) {
        return injector.getInstance(klass);
    }


}
