package com.tramchester.domain.collections;

import java.util.stream.Stream;

public class SimpleListSingleton<T> implements SimpleList<T> {
    private final T item;

    public SimpleListSingleton(T item) {
        this.item = item;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public Stream<T> stream() {
        return Stream.of(item);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public T item() {
        return item;
    }
}
