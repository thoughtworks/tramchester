package com.tramchester;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.DataLoadStrategy;
import com.tramchester.graph.graphbuild.GraphFilter;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.Caching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class PicoContainerDependencies extends ComponentContainer {
    private static final Logger logger = LoggerFactory.getLogger(PicoContainerDependencies.class);

    private final MutablePicoContainer picoContainer = new DefaultPicoContainer(new Caching());

    public <T extends DataLoadStrategy> PicoContainerDependencies(GraphFilter graphFilter, TramchesterConfig config, Class<T> override) {

    }

    @Override
    public void initContainer() {
        logger.info("initialise");

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

    public <C> Set<C> getAll(Class<C> klass) {
        return new HashSet<>(picoContainer.getComponents(klass));
    }

    public  <C> void addComponent(Class<C> instance) {
        picoContainer.addComponent(instance);
    }

    @Override
    protected <C> void addComponent(Class<C> klass, C instance) {
        picoContainer.addComponent(klass, instance);
    }

    @Override
    protected <I,C extends I> void addComponent(Class<I> face, Class<C> concrete) {
        picoContainer.addComponent(face, concrete);
    }

    protected void stop() {
        logger.info("Components stop");
        picoContainer.stop();
        logger.info("Components dispose");
        picoContainer.dispose();
    }

}
