package com.tramchester.domain.collections;

import java.util.BitSet;
import java.util.stream.IntStream;

public class BitmapImpl {
    private final BitSet bitSet;
    private final int size;

    public BitmapImpl(int size) {
        this(new BitSet(size), size);
    }

    public BitmapImpl(BitSet source, int size) {
        this.bitSet = source;
        this.size = size;
    }

    public BitmapImpl createCopy() {
        return new BitmapImpl((BitSet) bitSet.clone(), size);
    }

    public int cardinality() {
        return bitSet.cardinality();
    }

    public void clear() {
        bitSet.clear();
    }

    public BitmapImpl getSubmap(int start, int end) {
        BitSet partial = bitSet.get(start, end);
        return new BitmapImpl(partial, end-start);
    }

    public void set(int position) {
        bitSet.set(position);
    }

    public void set(int position, boolean value) {
        bitSet.set(position, value);
    }

    public void setAll(int start, int end) {
        bitSet.set(start, end);
    }

    public void setAll(int start, int end, boolean value) {
        bitSet.set(start, end, value);
    }

    public boolean get(int position) {
        return bitSet.get(position);
    }

    public void or(BitmapImpl other) {
        bitSet.or(other.bitSet);
    }

    public void and(BitmapImpl other) {
        bitSet.and(other.bitSet);
    }

    public void andNot(BitmapImpl contained) {
        bitSet.andNot(contained.bitSet);
    }

    public IntStream stream() {
        return bitSet.stream();
    }

    public boolean isEmpty() {
        return bitSet.isEmpty();
    }

    String display(int rows, int columns) {
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
