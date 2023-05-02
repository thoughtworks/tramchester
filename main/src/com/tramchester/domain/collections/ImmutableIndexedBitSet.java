package com.tramchester.domain.collections;

public interface ImmutableIndexedBitSet {
    ImmutableBitSet getBitSetForRow(int row);

    ImmutableBitSet createImmutable();

    long numberOfBitsSet();

    boolean isSet(RouteIndexPair pair);

    IndexedBitSet getCopyOfRowAndColumn(int row, int column);
}
