package com.tramchester.domain.collections;

import java.util.stream.Stream;

// TODO is this even helping?
public interface LazyList<T> {

    static <T> LazyList<T> concat(LazyList<T> listA, LazyList<T> listB) {
        return LazyListItems.create(listA, listB);
    }

    int size();

    Stream<T> stream();

    boolean isSingleton();
}
