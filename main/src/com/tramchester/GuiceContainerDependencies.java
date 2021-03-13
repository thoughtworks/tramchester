package com.tramchester;

import com.google.inject.*;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.lazy.LazySingletonScope;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.DefaultDataLoadStrategy;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.NodeIdLabelMap;
import com.tramchester.graph.NodeTypeRepository;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.TransportDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuiceContainerDependencies extends ComponentContainer {
    private static final Logger logger = LoggerFactory.getLogger(GuiceContainerDependencies.class);

    private final Module module;
    private final Injector injector;

    public <T extends TransportDataProvider> GuiceContainerDependencies(GraphFilter graphFilter, TramchesterConfig config,
                                                                        Class<T> overrideType,
                                                                        CacheMetrics.RegistersCacheMetrics registerMetrics) {
        this(graphFilter, config, new AlternateProviderModule<>(overrideType), registerMetrics);
    }

    public GuiceContainerDependencies(GraphFilter filter, TramchesterConfig config, CacheMetrics.RegistersCacheMetrics registerCacheMetrics) {
        this(filter, config, new DefaultProviderModule(), registerCacheMetrics);
    }

    private GuiceContainerDependencies(GraphFilter filter, TramchesterConfig config, AbstractModule providerModule,
                                       CacheMetrics.RegistersCacheMetrics registerCacheMetrics) {
        module = new Module(this, filter, config, registerCacheMetrics);
        injector = LifecycleInjector.builder().
                withModules(providerModule, module).
                build().createInjector();
    }

    @Override
    public void initContainer() {
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

    @Override
    protected void stop() {
        LifecycleManager manager = injector.getInstance(LifecycleManager.class);
        if (manager==null) {
            logger.error("Unable to get lifecycle manager for close()");
        } else {
            logger.info("Services Manager close");
            manager.close();
        }
    }

    protected void registerLinkedComponents() {
        // keep these visible, so easy to change to non-cahced version when needed
        module.bindClass(NodeContentsRepository.class, CachedNodeOperations.class);
        module.bindClass(NodeTypeRepository.class, NodeIdLabelMap.class);
    }

    @Override
    public <C> C get(Class<C> klass) {
        return injector.getInstance(klass);
    }

    @Override
    protected <C> void addComponent(Class<C> klass, C instance) {
        module.bindInstance(klass, instance);
    }

    @Override
    protected <I,C extends I> void addComponent(Class<I> face, Class<C> concrete) {
        module.bindClass(face, concrete);
    }

    public static class DefaultProviderModule extends AbstractModule {
        @Provides
        @Singleton
        public TransportDataProvider getDataProvider(DefaultDataLoadStrategy defaultDataLoadStrategy) {
            return defaultDataLoadStrategy.getProvider();
        }
    }

    public static class AlternateProviderModule<T extends TransportDataProvider> extends AbstractModule {
        private final Class<T> type;

        public AlternateProviderModule(Class<T> type) {
            this.type = type;
        }

        @Override
        protected void configure() {
            bind(TransportDataProvider.class).to(type).in(LazySingletonScope.get());
        }
    }

}
