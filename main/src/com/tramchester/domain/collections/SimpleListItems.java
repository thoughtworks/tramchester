package com.tramchester.domain.collections;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.stream.Stream;

public class SimpleListItems<T> implements SimpleList<T> {

    private final T[] theArray;
    private final Class<T> theClass;

    private SimpleListItems(Class<T> theClass, SimpleListSingleton<T> singletonA, SimpleListSingleton<T> singletonB) {
        theArray = createArray( theClass, 2);
        theArray[0] = singletonA.item();
        theArray[1] = singletonB.item();
        this.theClass = theClass;
    }

    public SimpleListItems(Class<T> theClass, SimpleListSingleton<T> singleton, SimpleListItems<T> list) {
        final int listSize = list.size();
        theArray = createArray( theClass, listSize +1);
        theArray[0] = singleton.item();
        System.arraycopy(list.theArray, 0, theArray, 1, listSize);
        this.theClass = theClass;
    }

    public SimpleListItems(Class<T> theClass, SimpleListItems<T> list, SimpleListSingleton<T> singleton) {
        this(theClass, list, singleton.item());
//        final int listSize = list.size();
//        theArray = createArray( theClass, listSize +1);
//        System.arraycopy(list.theArray, 0, theArray, 0, listSize);
//        theArray[listSize] = singleton.item();
//        this.theClass = theClass;
    }

    public SimpleListItems(Class<T> theClass, SimpleListItems<T> list, T item) {
        final int listSize = list.size();
        theArray = createArray( theClass, listSize +1);
        System.arraycopy(list.theArray, 0, theArray, 0, listSize);
        theArray[listSize] = item;
        this.theClass = theClass;
    }

    public SimpleListItems(Class<T> theClass, SimpleListItems<T> listA, SimpleListItems<T> listB) {
        int listASize = listA.size();
        int listBSize = listB.size();
        theArray = createArray( theClass, listASize + listBSize);
        System.arraycopy(listA.theArray, 0, theArray, 0, listASize);
        System.arraycopy(listB.theArray, 0, theArray, listASize, listBSize);
        this.theClass = theClass;
    }

    SimpleListItems(Class<T> theClass, T itemA, T itemB) {
        this.theClass = theClass;
        theArray = createArray( theClass, 2);
        theArray[0] = itemA;
        theArray[1] = itemB;
    }


    @Override
    public SimpleList<T> concat(SimpleList<T> other) {
        if (other.isSingleton()) {
            final SimpleListSingleton<T> singleton = (SimpleListSingleton<T>) other;
            return concat(singleton.item());
        } else {
            return new SimpleListItems<>(other.getKlass(), this, (SimpleListItems<T>) other);
        }
    }

    private T[] createArray(Class<T> theClass, int size) {
        return (T[]) Array.newInstance(theClass, size);
    }

    public static <T> SimpleList<T> create(Class<T> theClass, SimpleList<T> listA, SimpleList<T> listB) {
        boolean aSingleton = listA.isSingleton();
        boolean bSingleton = listB.isSingleton();
        if (aSingleton && bSingleton) {
            return new SimpleListItems<T>(theClass, (SimpleListSingleton<T>) listA, (SimpleListSingleton<T>) listB);
        }
        if (aSingleton) {
            return new SimpleListItems<T>(theClass, (SimpleListSingleton<T>) listA, (SimpleListItems<T>)listB);
        }
        if (bSingleton) {
            return new SimpleListItems<T>(theClass, (SimpleListItems<T>) listA, (SimpleListSingleton<T>)listB);
        }
        return new SimpleListItems<T>(theClass, (SimpleListItems<T>) listA, (SimpleListItems<T>)listB);
    }

    @Override
    public int size() {
        return theArray.length;
    }

    public Stream<T> stream() {
        return Arrays.stream(theArray);
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public SimpleList<T> concat(T item) {
        return new SimpleListItems<T>(theClass, this, item);
    }

    @Override
    public Class<T> getKlass() {
        return theClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleListItems<?> that = (SimpleListItems<?>) o;
        return Arrays.equals(theArray, that.theArray);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(theArray);
    }
}
