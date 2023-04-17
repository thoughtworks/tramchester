package com.tramchester.domain.collections;

import java.util.stream.IntStream;

public interface SimpleBitmap {
    SimpleBitmap createCopy();

    int cardinality();

    void clear();

    SimpleBitmap getSubmap(int start, int end);

    void set(int position);

    void set(int position, boolean value);

    void setAll(int start, int end);

    void setAll(int start, int end, boolean value);

    boolean get(int position);

    void or(SimpleBitmap other);

    void and(SimpleBitmap other);

    void andNot(SimpleBitmap contained);

    IntStream stream();

    boolean isEmpty();

    String displayAs(int rows, int columns);
}
