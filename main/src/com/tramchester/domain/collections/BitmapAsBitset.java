package com.tramchester.domain.collections;

import java.util.BitSet;
import java.util.stream.IntStream;

public class BitmapAsBitset implements SimpleBitmap {
    private final BitSet bitSet;
    private final int size;

    public BitmapAsBitset(int size) {
        this(new BitSet(size), size);
    }

    public BitmapAsBitset(BitSet source, int size) {
        this.bitSet = source;
        this.size = size;
    }

    @Override
    public BitmapAsBitset createCopy() {
        return new BitmapAsBitset((BitSet) bitSet.clone(), size);
    }

    @Override
    public int cardinality() {
        return bitSet.cardinality();
    }

    @Override
    public void clear() {
        bitSet.clear();
    }

    @Override
    public BitmapAsBitset getSubmap(int start, int end) {
        BitSet partial = bitSet.get(start, end);
        return new BitmapAsBitset(partial, end-start);
    }

    @Override
    public void set(int position) {
        bitSet.set(position);
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
    public void or(SimpleBitmap other) {
        BitmapAsBitset otherBitmap = (BitmapAsBitset) other;
        bitSet.or(otherBitmap.bitSet);
    }

    @Override
    public void and(SimpleBitmap other) {
        BitmapAsBitset otherBitmap = (BitmapAsBitset) other;
        bitSet.and(otherBitmap.bitSet);
    }

    @Override
    public void andNot(SimpleBitmap other) {
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
    public String displayAs(int rows, int columns) {
        StringBuilder result = new StringBuilder();
        result.append(System.lineSeparator());
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                char bit = bitSet.get(getPositionFor(row, column, rows, columns)) ? '1' : '0';
                result.append(bit);
            }
            result.append(System.lineSeparator());
        }
        return result.toString();
    }

    private int getPositionFor(int row, int column, int rows, int columns) {
        if (row >= rows) {
            throw new RuntimeException("Row is out of bounds, more than " + rows);
        }
        if (column >= columns) {
            throw new RuntimeException("Column is out of bounds, more than " + columns);

        }
        return (row * columns) + column;
    }
}
