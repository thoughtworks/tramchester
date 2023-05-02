package com.tramchester.domain.collections;

public interface ImmutableIndexedBitSet {

    SimpleImmutableBitmap getBitSetForRow(int row);

    long numberOfBitsSet();

    boolean isSet(RouteIndexPair pair);

    IndexedBitSet getCopyOfRowAndColumn(int row, int column);

}
