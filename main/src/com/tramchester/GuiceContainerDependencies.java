package com.tramchester;

import com.google.inject.*;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GuiceContainerDependencies extends ComponentContainer {
    private static final Logger logger = LoggerFactory.getLogger(GuiceContainerDependencies.class);

    private final GuiceFacade module;
    private final Injector injector;
    private TransportData transportData;

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
        injector = Guice.createInjector(module);
    }

    @Override
    public void initialise(TransportDataProvider transportDataProvider) {
        logger.info("initialise");

        this.transportData = transportDataProvider.getData();

        if (logger.isDebugEnabled()) {
            logger.warn("Debug logging is enabled, server performance will be impacted");
        }

        logger.info("Start components");

        // TODO
        //picoContainer.start();
    }

    private void registerTransportDataProvider() {
        Provider<TransportData> provider = () -> transportData;
        module.bindProvider(TransportData.class, provider);
    }


    @Override
    public <T> T get(Class<T> klass) {
        return injector.getInstance(klass);
    }

    @Override
    protected <T> List<T> getAll(Class<T> klass) {
        Map<Key<?>, Binding<?>> all = injector.getAllBindings();
        List<T> matched = all.entrySet().stream().
                filter(entry -> klass.isAssignableFrom(entry.getValue().getProvider().get().getClass())).
                map(entry -> entry.getKey().getClass()).
                map(key -> (T) get(key)).
                collect(Collectors.toList());
        return matched;
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

    @Override
    protected void stop() {
        // TODO
    }
}
