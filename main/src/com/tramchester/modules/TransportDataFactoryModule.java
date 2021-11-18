package com.tramchester.modules;

import com.google.inject.AbstractModule;
import com.tramchester.dataimport.loader.TransportDataFactory;

public class TransportDataFactoryModule <FACTORY extends TransportDataFactory>  extends AbstractModule  {
    private final Class<FACTORY> factoryType;

    public TransportDataFactoryModule(Class<FACTORY> factoryType) {
        this.factoryType = factoryType;
    }

    @Override
    protected void configure() {
        bind(TransportDataFactory.class).to(factoryType);
    }
}
