package com.tramchester.domain.collections;

import java.util.stream.IntStream;

public interface SimpleImmutableBitmap {

    SimpleImmutableBitmap getSubmap(int start, int end);

    boolean get(int position);

    IntStream stream();

    boolean isEmpty();

    int size();

    long cardinality();

}
