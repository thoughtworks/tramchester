package com.tramchester.domain.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class LazyListItems<T> implements LazyList<T> {

    private final List<T> theList;
    private final int hashCode;

    private LazyListItems(LazyListSingleton<T> singletonA, LazyListSingleton<T> singletonB) {
        theList = List.of(singletonA.item(), singletonB.item());
        hashCode = Objects.hash(theList);
    }

    public LazyListItems(LazyListSingleton<T> singleton, LazyListItems<T> list) {
        theList = new ArrayList<>(list.size()+1);
        theList.addAll(list.theList);
        theList.add(singleton.item());
        hashCode = Objects.hash(theList);
    }

    public LazyListItems(LazyListItems<T> list, LazyListSingleton<T> singleton) {
        theList = new ArrayList<T>(list.theList);
        theList.add(singleton.item());
        hashCode = Objects.hash(theList);
    }

    public LazyListItems(LazyListItems<T> listA, LazyListItems<T> listB) {
        theList = new ArrayList<T>(listA.theList);
        theList.addAll(listB.theList);
        hashCode = Objects.hash(theList);
    }

    public static <T> LazyList<T> create(LazyList<T> listA, LazyList<T> listB) {
        boolean aSingleton = listA.isSingleton();
        boolean bSingleton = listB.isSingleton();
        if (aSingleton && bSingleton) {
            return new LazyListItems<T>((LazyListSingleton<T>) listA, (LazyListSingleton<T>) listB);
        }
        if (aSingleton) {
            return new LazyListItems<T>((LazyListSingleton<T>) listA, (LazyListItems<T>)listB);
        }
        if (bSingleton) {
            return new LazyListItems<T>((LazyListItems<T>) listA, (LazyListSingleton<T>)listB);
        }
        return new LazyListItems<T>((LazyListItems<T>) listA, (LazyListItems<T>)listB);
    }

    @Override
    public int size() {
        return theList.size();
    }

    public Stream<T> stream() {
        return theList.stream();
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LazyListItems<?> that = (LazyListItems<?>) o;
        return theList.equals(that.theList);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
