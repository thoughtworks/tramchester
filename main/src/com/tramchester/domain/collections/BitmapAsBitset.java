package com.tramchester.domain.collections;

import java.util.BitSet;
import java.util.stream.IntStream;

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
