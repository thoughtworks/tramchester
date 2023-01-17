package com.tramchester.domain.collections;

import java.util.BitSet;
import java.util.stream.IntStream;

public class ImmutableBitSet {
    private final BitSet contained;
    private final int size;

    public ImmutableBitSet(BitSet contained, int size) {
        this.contained = contained;
        this.size = size;
    }

    public void applyOrTo(BitSet mutable) {
        mutable.or(contained);
    }

    public void applyAndNotTo(BitSet mutable) {
        mutable.andNot(contained);
    }

    public boolean isSet(int index) {
        if (index>size) {
            throw new RuntimeException("index " + index + " out of range for size " + size);
        }
        return contained.get(index);
    }

    public IntStream getBitIndexes() {
        return contained.stream();
    }

    public BitSet getContained() {
        return contained;
    }

    public int numberSet() {
        return contained.cardinality();
    }

    @Override
    public String toString() {
        return "ImmutableBitSet{" +
                "contained=" + contained +
                '}';
    }

    public boolean isEmpty() {
        return contained.isEmpty();
    }

    public IntStream getBitsSet() {
        return contained.stream();
    }


    public int getSize() {
        // using bitmap size can yield unexpected result as it gives the currently allocated size
        return size;
    }

}
