package com.tramchester.domain.collections;

import java.util.BitSet;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ImmutableBitSet {
    private final BitSet contained;

    public ImmutableBitSet(BitSet contained) {
        this.contained = contained;
    }

    public void applyOr(BitSet mutable) {
        mutable.or(contained);
    }

    public boolean get(int index) {
        return contained.get(index);
    }

    public void applyAndNot(BitSet mutable) {
        mutable.andNot(contained);
    }

    public ImmutableBitSet and(ImmutableBitSet immutableBitSet) {
        BitSet mutable = new BitSet(contained.size());
        mutable.or(contained);
        mutable.and(immutableBitSet.contained);
        return new ImmutableBitSet(mutable);
    }

    public IntStream stream() {
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

    // note: size is misleading for BitSet as it is allocated size which might not match the requested size
//    public int size() {
//        return contained.size();
//    }
}
