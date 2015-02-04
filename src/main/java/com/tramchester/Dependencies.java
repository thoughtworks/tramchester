package com.tramchester;


import com.tramchester.config.AppConfiguration;
import com.tramchester.resources.TestResource;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.Caching;

public class Dependencies {
    protected final MutablePicoContainer picoContainer = new DefaultPicoContainer(new Caching());

    public void initialise(AppConfiguration configuration) {
        picoContainer.addComponent(AppConfiguration.class, configuration);

        picoContainer.addComponent(TestResource.class);
    }

    

    public <T> T get(Class<T> klass) {
        return picoContainer.getComponent(klass);
    }

}
