package com.tramchester.domain.collections;

import java.util.Objects;
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

    @Override
    public SimpleList<T> concat(SimpleList<T> other) {
        if  (other.isSingleton()) {
            SimpleListSingleton<T> otherSingleton = (SimpleListSingleton<T>) other;
            return new SimpleListItems<>(this, otherSingleton);
        } else {
            return other.concat(this);
        }
    }

    public T item() {
        return item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleListSingleton<?> that = (SimpleListSingleton<?>) o;
        return item.equals(that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item);
    }

    @Override
    public String toString() {
        return "{" +
                "item=" + item +
                '}';
    }
}
