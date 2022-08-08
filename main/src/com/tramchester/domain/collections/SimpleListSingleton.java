package com.tramchester.domain.collections;

import java.util.stream.Stream;

public class SimpleListSingleton<T> implements SimpleList<T> {
    private final T item;
    private final Class<T> theClass;

    public SimpleListSingleton(Class<T> theClass, T item) {
        this.theClass = theClass;
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
            return new SimpleListItems<T>(theClass, this.item, otherSingleton.item);
        } else {
            return other.concat(this.item);
        }
    }

    @Override
    public SimpleList<T> concat(T otherItem) {
        return new SimpleListItems<T>(theClass, this.item, otherItem);
    }

    @Override
    public Class<T> getKlass() {
        return theClass;
    }

    public T item() {
        return item;
    }


}
