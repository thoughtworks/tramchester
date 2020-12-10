package com.tramchester;

import com.google.inject.*;
import com.google.inject.matcher.Matcher;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleListener;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.governator.lifecycle.LifecycleState;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.NodeIdLabelMap;
import com.tramchester.graph.NodeTypeRepository;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GuiceContainerDependencies extends ComponentContainer {
    private static final Logger logger = LoggerFactory.getLogger(GuiceContainerDependencies.class);

    private final GuiceFacade module;
    private final Injector injector;
    private TransportData transportData;
    private LifecycleManager manager;

    public static class GuiceFacade extends AbstractModule {
        private final GuiceContainerDependencies parent;
        private final GraphFilter filter;
        private final TramchesterConfig config;

        public GuiceFacade(GuiceContainerDependencies parent, GraphFilter filter, TramchesterConfig config) {
            this.parent = parent;
            this.filter = filter;
            this.config = config;
        }

        @Override
        protected void configure() {
            parent.registerLinkedComponents();
            parent.registerTransportDataProvider();
            parent.registerComponents(config, filter);
        }

        public <I,T extends I> void bindClass(Class<I> face, Class<T> klass) {
            bind(face).to(klass);
        }

        public <T> void bindInstance(Class<T> klass, T instance) {
            bind(klass).toInstance(instance);
        }

        public <T> void bindProvider(Class<T> klass, Provider<T> provider) {
            bind(klass).toProvider(provider);
        }
    }

    public GuiceContainerDependencies(GraphFilter config, TramchesterConfig filter) {
        module = new GuiceFacade(this, config, filter);

        BootstrapModule bootstrapModule = binder -> binder.bindLifecycleListener().to(LifecycleListen.class);

        injector = LifecycleInjector.builder().withBootstrapModule(bootstrapModule).withModules(module).build().createInjector();
    }

    @Override
    public void initialise(TransportDataProvider transportDataProvider) {
        logger.info("initialise");

        this.transportData = transportDataProvider.getData();

        if (logger.isDebugEnabled()) {
            logger.warn("Debug logging is enabled, server performance will be impacted");
        }

        logger.info("Start components");
        manager = injector.getInstance(LifecycleManager.class);
        try {
            manager.start();
        } catch (Exception e) {
            logger.error("Failed to start", e);
        }

        get(StagedTransportGraphBuilder.class);

        logger.info("Done");
    }

    @Override
    protected void stop() {
        manager.close();
    }

    private void registerTransportDataProvider() {
        Provider<TransportData> provider = () -> transportData;
        module.bindProvider(TransportData.class, provider);
    }

    private void registerLinkedComponents() {
        // keep these visible, so easy to change to non-cahced version when needed
        module.bindClass(NodeContentsRepository.class, CachedNodeOperations.class);
        module.bindClass(NodeTypeRepository.class, NodeIdLabelMap.class);
    }

    @Override
    public <T> T get(Class<T> klass) {
        return injector.getInstance(klass);
    }

    @Override
    protected <T> List<T> getAll(Class<T> klass) {
        Map<Key<?>, Binding<?>> all = injector.getAllBindings();
        Set<TypeLiteral<?>> found = all.entrySet().stream().
                filter(entry -> klass.isAssignableFrom(entry.getValue().getProvider().get().getClass())).
                map(entry -> entry.getKey().getTypeLiteral()).
                collect(Collectors.toSet());
        //noinspection unchecked
        return found.stream().
                map(literal -> get(literal.getRawType())).
                filter(instance -> klass.isAssignableFrom(instance.getClass())).
                map(instance -> (T)instance).
                collect(Collectors.toList());
    }

    @Override
    protected <T> void addComponent(Class<T> klass) {
        // no-op for Guice
        // module.bindClass(klass);
    }

    @Override
    protected <T> void addComponent(Class<T> klass, T instance) {
        module.bindInstance(klass, instance);
    }

    @Override
    protected <I,T extends I> void addComponent(Class<I> face, Class<T> concrete) {
        module.bindClass(face, concrete);
    }

    // for debug
    private static class LifecycleListen implements LifecycleListener {

        public LifecycleListen() {

        }

        @Override
        public <T> void objectInjected(TypeLiteral<T> type, T obj) {
            logger.info("Injected " + type.getRawType().getCanonicalName());
        }

        @Override
        public <T> void objectInjected(TypeLiteral<T> type, T obj, long duration, TimeUnit units) {
            logger.info("Injected " + type.getRawType().getCanonicalName());
        }

        @Override
        public void stateChanged(Object obj, LifecycleState newState) {

        }

        @Override
        public <T> void objectInjecting(TypeLiteral<T> type) {

        }
    }
}
