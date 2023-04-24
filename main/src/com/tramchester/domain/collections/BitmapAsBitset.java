package com.tramchester.domain.collections;

import java.util.BitSet;
import java.util.stream.IntStream;

import static java.lang.String.format;

public class BitmapAsBitset implements SimpleBitmap {
    private final BitSet bitSet;
    private final int size;

    BitmapAsBitset(int size) {
        this(new BitSet(size), size);
    }

    private BitmapAsBitset(BitSet source, int size) {
        this.bitSet = source;
        this.size = size;
    }

    @Override
    public SimpleBitmap createCopy() {
        return new BitmapAsBitset((BitSet) bitSet.clone(), size);
    }

    @Override
    public SimpleBitmap extractRowAndColumn(int row, int column, int totalRows, int totalColumns) {
        if (totalRows*totalColumns!=size) {
            throw new RuntimeException(format("Cannot apply, size mismatch, size is %s, totalRows=%s totalColumns=%s",
                    size, totalRows, totalColumns));
        }

        BitSet mask = createMask(row, column, totalRows, totalColumns);
        BitSet result = (BitSet) bitSet.clone();
        result.and(mask);
        return new BitmapAsBitset(result, size);
    }

    private static BitSet createMask(int row, int column, int totalRows, int totalColumns) {
        BitSet result = new BitSet();

        int rowStart = getPositionFor(row, 0, totalColumns);
        result.set(rowStart, rowStart+totalColumns);

        for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
            int columnPosition = getPositionFor(rowIndex, column, totalColumns);
            result.set(columnPosition);
        }

        return result;
    }

    private static int getPositionFor(int row, int column, int totalColumns) {
        return (row * totalColumns) + column;
    }

    @Override
    public long cardinality() {
        return bitSet.cardinality();
    }

    @Override
    public void clear() {
        bitSet.clear();
    }

    @Override
    public SimpleImmutableBitmap getSubmap(int start, int end) {
        BitSet partial = bitSet.get(start, end);
        return new BitmapAsBitset(partial, end-start);
    }

    @Override
    public void set(int position) {
        bitSet.set(position);
    }

    @Override
    public void set(int[] positionsToSet) {
        for (int toSet : positionsToSet) {
            set(toSet);
        }
    }

    @Override
    public void set(int position, boolean value) {
        bitSet.set(position, value);
    }

    @Override
    public void setAll(int start, int end) {
        bitSet.set(start, end);
    }

    @Override
    public void setAll(int start, int end, boolean value) {
        bitSet.set(start, end, value);
    }

    @Override
    public boolean get(int position) {
        return bitSet.get(position);
    }

    @Override
    public void or(SimpleImmutableBitmap other) {
        BitmapAsBitset otherBitmap = (BitmapAsBitset) other;
        bitSet.or(otherBitmap.bitSet);
    }

    @Override
    public void and(SimpleImmutableBitmap other) {
        BitmapAsBitset otherBitmap = (BitmapAsBitset) other;
        bitSet.and(otherBitmap.bitSet);
    }

    @Override
    public void andNot(SimpleImmutableBitmap other) {
        BitmapAsBitset otherBitmap = (BitmapAsBitset) other;
        bitSet.andNot(otherBitmap.bitSet);
    }

    @Override
    public IntStream stream() {
        return bitSet.stream();
    }

    @Override
    public boolean isEmpty() {
        return bitSet.isEmpty();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public String toString() {
        return "BitmapAsBitset{" +
                "bitSet=" + bitSet +
                ", size=" + size +
                '}';
    }
}
