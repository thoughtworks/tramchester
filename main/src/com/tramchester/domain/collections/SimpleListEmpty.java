package com.tramchester.domain.collections;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/***
 * Primarily here to support Collector on streams
 */
public class SimpleListEmpty<T> implements SimpleList<T> {
    @Override
    public int size() {
        return 0;
    }

    @Override
    public Stream<T> stream() {
        return Stream.empty();
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public SimpleList<T> concat(SimpleList<T> other) {
        return other;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public List<T> toList() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "[]";
    }
}
