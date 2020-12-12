package com.tramchester;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.healthchecks.RegistersHealthchecks;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import java.util.Set;
import java.util.stream.Collectors;

// TODO collapse classes as now only one implementation
public abstract class ComponentContainer {
    private static final Logger logger = LoggerFactory.getLogger(ComponentContainer.class);

    private final Reflections reflections;

    // init dependencies but possibly with alternative source of transport data
    public abstract void initContainer();

    public abstract <T> T get(Class<T> klass);

    protected abstract <T> void addComponent(Class<T> klass, T instance);

    protected abstract <I,T extends I> void addComponent(Class<I> face, Class<T> concrete);

    protected abstract void stop();

    public void initialise() {
        logger.info("Init");
        initContainer();
    }

    public ComponentContainer() {
        reflections = new Reflections(App.class.getPackageName());
    }

    public Set<?> getResources() {
        Set<Class<?>> types = reflections.getTypesAnnotatedWith(Path.class);
        return types.stream().map(this::get).collect(Collectors.toSet());
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

    protected void registerConfiguration(TramchesterConfig configuration, GraphFilter graphFilter,
                                         CacheMetrics.RegistersMetrics registersMetrics) {
        addComponent(ProvidesNow.class, ProvidesLocalNow.class);
        addComponent(TramchesterConfig.class, configuration);
        addComponent(GraphFilter.class, graphFilter);
        addComponent(CacheMetrics.RegistersMetrics.class, registersMetrics);
    }

}
