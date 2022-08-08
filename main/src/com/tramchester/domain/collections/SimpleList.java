package com.tramchester.domain.collections;

import java.util.stream.Stream;

// TODO is this even helping?
public interface SimpleList<T> {

    static <T> SimpleList<T> concat(SimpleList<T> listA, SimpleList<T> listB) {
        return listA.concat(listB);
    }

    int size();

    Stream<T> stream();

    boolean isSingleton();

    SimpleList<T> concat(SimpleList<T> other);
}
