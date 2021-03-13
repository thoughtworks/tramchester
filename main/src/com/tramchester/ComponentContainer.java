package com.tramchester;

public interface ComponentContainer {
    void initialise();
    <C> C get(Class<C> klass);
    void close();

}
