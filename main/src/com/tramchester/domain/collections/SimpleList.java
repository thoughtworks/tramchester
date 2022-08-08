package com.tramchester.domain.collections;

import java.util.stream.Stream;

// TODO is this even helping?
public interface SimpleList<T> {

    static <T> SimpleList<T> concat(Class<T> theClass, SimpleList<T> listA, SimpleList<T> listB) {
        return SimpleListItems.create(theClass, listA, listB);
    }

    int size();

    Stream<T> stream();

    boolean isSingleton();

    SimpleList<T> concat(SimpleList<T> other);

    SimpleList<T> concat(T item);

    Class<T> getKlass();
}
