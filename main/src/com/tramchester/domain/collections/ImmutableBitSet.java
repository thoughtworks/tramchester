package com.tramchester.domain.collections;

import java.util.BitSet;
import java.util.stream.IntStream;

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
}
