package com.tramchester.domain;

import com.tramchester.graph.search.routes.RouteIndexPair;

import java.util.BitSet;

import static java.lang.String.format;

public class IndexedBitSet {
    private final int numberOfRoutes;
    private final BitSet bitSet;
    private final int totalSize;

    public IndexedBitSet(int numberOfRoutes) {
        this.numberOfRoutes = numberOfRoutes;
        totalSize = numberOfRoutes * numberOfRoutes;
        bitSet = new BitSet(totalSize);
    }

    private IndexedBitSet(int numberOfRoutes, BitSet bitSet) {
        this.numberOfRoutes = numberOfRoutes;
        totalSize = numberOfRoutes * numberOfRoutes;
        this.bitSet = bitSet;
    }

    public static IndexedBitSet getIdentity(int size) {
        IndexedBitSet result = new IndexedBitSet(size);
        result.bitSet.set(0, result.totalSize);
        return result;
    }

    public void set(int indexA, int indexB) {
        int position = getPositionFor(indexA, indexB);
        bitSet.set(position);
    }

    public boolean isSet(int indexA, int indexB) {
        int position = getPositionFor(indexA, indexB);
        return bitSet.get(position);
    }

    public ImmutableBitSet getBitSetForRow(int routesIndex) {
        int startPosition = getPositionFor(routesIndex, 0);
        int endPosition = startPosition + numberOfRoutes;
        BitSet result = bitSet.get(startPosition, endPosition);

        return new ImmutableBitSet(result);
    }

    public void insert(int routeIndex, BitSet connectionsForRoute) {
        int startPosition = getPositionFor(routeIndex, 0);
        for (int i = 0; i < numberOfRoutes; i++) {
            bitSet.set(startPosition + i, connectionsForRoute.get(i));
        }
    }

    public int numberOfConnections() {
        return bitSet.cardinality();
    }

    public void clear() {
        bitSet.clear();
    }

    public void applyAndTo(int index, BitSet row) {
        int startPosition = getPositionFor(index, 0);

        // TODO more efficient ways to do this via a mask?
        for (int i = 0; i < numberOfRoutes; i++) {
            int bitIndex = startPosition + i;
            boolean andValue = bitSet.get(bitIndex) && row.get(i);
            bitSet.set(bitIndex, andValue);
        }
    }

    private int getPositionFor(int indexA, int indexB) {
        return (indexA * numberOfRoutes) + indexB;
    }

    public IndexedBitSet and(IndexedBitSet other) {
        if (numberOfRoutes != other.numberOfRoutes) {
            throw new RuntimeException(format("Mismatch on matrix size this %s other %s", numberOfRoutes, other.numberOfRoutes));
        }
        BitSet cloned = (BitSet) this.bitSet.clone();
        cloned.and(other.bitSet);
        return new IndexedBitSet(numberOfRoutes, bitSet);
    }

    public boolean isSet(RouteIndexPair pair) {
        return isSet(pair.first(), pair.second());
    }
}
