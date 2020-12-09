package com.tramchester;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataProvider;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.picocontainer.behaviors.Caching;
import org.picocontainer.injectors.FactoryInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;

public class PicoContainerDependencies extends ComponentContainer {
    private static final Logger logger = LoggerFactory.getLogger(PicoContainerDependencies.class);

    private final MutablePicoContainer picoContainer = new DefaultPicoContainer(new Caching());

    public PicoContainerDependencies(GraphFilter graphFilter, TramchesterConfig configuration) {
        logger.info("construct");
        registerComponents(configuration, graphFilter);
    }

    @Override
    public void initialise(TransportDataProvider transportDataProvider) {
        logger.info("initialise");

        picoContainer.addAdapter(new FactoryInjector<TransportData>() {
            @Override
            public TransportData getComponentInstance(PicoContainer container, Type into) {
                return transportDataProvider.getData();
            }
        });

        if (logger.isDebugEnabled()) {
            logger.warn("Debug logging is enabled, server performance will be impacted");
        }
        
        logger.info("Start components");
        picoContainer.start();
    }

    @Override
    public <T> T get(Class<T> klass) {

        T component = picoContainer.getComponent(klass);
        if (component==null) {
            logger.warn("Missing dependency " + klass);
        }
        return component;
    }

    public <T> List<T> getAll(Class<T> klass) {
        return picoContainer.getComponents(klass);
    }

    public  <T> void addComponent(Class<T> instance) {
        picoContainer.addComponent(instance);
    }

    @Override
    protected <T> void addComponent(Class<T> klass, T instance) {
        picoContainer.addComponent(klass, instance);
    }

    @Override
    protected <I,T extends I> void addComponent(Class<I> face, Class<T> concrete) {
        picoContainer.addComponent(face, concrete);
    }

    protected void stop() {
        logger.info("Components stop");
        picoContainer.stop();
        logger.info("Components dispose");
        picoContainer.dispose();
    }

}
