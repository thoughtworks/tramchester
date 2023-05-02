package com.tramchester.domain.collections;

import java.util.stream.Stream;

public interface SimpleImmutableBitmap {

    boolean get(int position);

    boolean isEmpty();

    int size();

    long cardinality();

    SimpleImmutableBitmap getSubmap(int start, int end);

    Stream<Short> getBitIndexes();

}
