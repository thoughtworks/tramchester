package com.tramchester;

import com.google.inject.*;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleListener;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.governator.lifecycle.LifecycleState;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.DefaultDataLoadStrategy;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.NodeIdLabelMap;
import com.tramchester.graph.NodeTypeRepository;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.repository.TransportDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GuiceContainerDependencies extends ComponentContainer {
    private static final Logger logger = LoggerFactory.getLogger(GuiceContainerDependencies.class);

    private final Module module;
    private final Injector injector;

//    public GuiceContainerDependencies(GraphFilter graphFilter, TramchesterConfig config, TransportDataProvider override) {
//        this(graphFilter, config, new OverrideProviderModule(override));
//    }

    public <T extends TransportDataProvider> GuiceContainerDependencies(GraphFilter graphFilter, TramchesterConfig config,
                                                                        Class<T> overrideType) {
        this(graphFilter, config, new AlternateProviderModule<>(overrideType));
    }

    public GuiceContainerDependencies(GraphFilter filter, TramchesterConfig config) {
        this(filter, config, new DefaultProviderModule());
    }

    private GuiceContainerDependencies(GraphFilter filter, TramchesterConfig config, AbstractModule providerModule) {
        module = new Module(this, filter, config);
        BootstrapModule bootstrapModule = binder -> binder.bindLifecycleListener().to(LifecycleListen.class);
        injector = LifecycleInjector.builder().
                withBootstrapModule(bootstrapModule).
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

        get(StationLocations.class);
        get(StagedTransportGraphBuilder.class);

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

    // TODO Use annotations instead? Easier to search for
    @Override
    protected <C> Set<C> getAll(Class<C> klass) {
        Map<Key<?>, Binding<?>> all = injector.getAllBindings();
        Set<TypeLiteral<?>> found = all.entrySet().stream().
                filter(entry -> klass.isAssignableFrom(classForEntry(entry))).
                map(entry -> entry.getKey().getTypeLiteral()).
                collect(Collectors.toSet());

        // note: needs to be a set, there are duplciated instances where multiple interfaces have same ImplementedBy
        //noinspection unchecked
        return found.stream().
                map(literal -> get(literal.getRawType())).
                filter(instance -> klass.isAssignableFrom(instance.getClass())).
                map(instance -> (C) instance).
                collect(Collectors.toSet());
    }

    private Class<?> classForEntry(Map.Entry<Key<?>, Binding<?>> entry) {
        return entry.getValue().getProvider().get().getClass();
    }

    @Override
    protected <C> void addComponent(Class<C> klass) {
        // TODO remove callers
         //module.bindSingle(klass);
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
            bind(TransportDataProvider.class).to(type); //.in(Scopes.SINGLETON);
        }
    }

//    public static class OverrideProviderModule extends AbstractModule {
//        private final TransportDataProvider provider;
//
//        public OverrideProviderModule(TransportDataProvider override) {
//            provider = override;
//        }
//
//        @Provides
//        @Singleton
//        public TransportDataProvider getDataProvider() {
//            return provider;
//        }
//    }

    // for debug
    private static class LifecycleListen implements LifecycleListener {

        public LifecycleListen() {

        }

        @Override
        public <T> void objectInjected(TypeLiteral<T> type, T obj) {
            logger.debug("Injected " + type.getRawType().getCanonicalName());
        }

        @Override
        public <T> void objectInjected(TypeLiteral<T> type, T obj, long duration, TimeUnit units) {
            logger.debug("Injected " + type.getRawType().getCanonicalName());
        }

        @Override
        public void stateChanged(Object obj, LifecycleState newState) {

        }

        @Override
        public <T> void objectInjecting(TypeLiteral<T> type) {

        }
    }
}
