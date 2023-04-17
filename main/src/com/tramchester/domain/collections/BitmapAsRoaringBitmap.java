package com.tramchester.domain.collections;

import org.roaringbitmap.buffer.MutableRoaringBitmap;

import java.util.stream.IntStream;

public class BitmapAsRoaringBitmap implements SimpleBitmap {
    private final MutableRoaringBitmap bitmap;
    private final int size;

    BitmapAsRoaringBitmap(int size) {
        this(new MutableRoaringBitmap(), size);
    }

    private BitmapAsRoaringBitmap(MutableRoaringBitmap source, int size) {
        this.bitmap = source;
        this.size = size;
    }

    @Override
    public SimpleBitmap createCopy() {
        return new BitmapAsRoaringBitmap(bitmap.clone(), size);
    }

    @Override
    public int cardinality() {
        return bitmap.getCardinality();
    }

    @Override
    public void clear() {
        bitmap.clear();
    }

    @Override
    public SimpleBitmap getSubmap(int start, int end) {
        int size = end - start;
        MutableRoaringBitmap partial = new MutableRoaringBitmap();

        int[] toSet = bitmap.stream().
                filter(n -> n >= start).
                filter(n -> n <= end)
                .map(n -> n - start).
                toArray();

        partial.addN(toSet, 0, toSet.length);
        return new BitmapAsRoaringBitmap(partial, size);
    }

    @Override
    public void set(int position) {
        bitmap.add(position);
    }

    @Override
    public void set(int position, boolean value) {
        if (value) {
            bitmap.add(position);
        } else {
            bitmap.remove(position);
        }
    }

    @Override
    public void setAll(int start, int end) {
        bitmap.add(start, end);
    }

    @Override
    public void setAll(int start, int end, boolean value) {
        if (value) {
            setAll(start, end);
        } else {
            bitmap.remove(start, end);
        }
    }

    @Override
    public boolean get(int position) {
        return bitmap.contains(position);
    }

    @Override
    public void or(SimpleBitmap other) {
        BitmapAsRoaringBitmap otherBitmap = (BitmapAsRoaringBitmap) other;
        bitmap.or(otherBitmap.bitmap);
    }

    @Override
    public void and(SimpleBitmap other) {
        BitmapAsRoaringBitmap otherBitmap = (BitmapAsRoaringBitmap) other;
        bitmap.and(otherBitmap.bitmap);
    }

    @Override
    public void andNot(SimpleBitmap other) {
        BitmapAsRoaringBitmap otherBitmap = (BitmapAsRoaringBitmap) other;
        bitmap.andNot(otherBitmap.bitmap);
    }

    @Override
    public IntStream stream() {
        return bitmap.stream();
    }

    @Override
    public boolean isEmpty() {
        return bitmap.isEmpty();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public String toString() {
        return "BitmapAsRoaringBitmap{" +
                "bitmap=" + bitmap +
                ", size=" + size +
                '}';
    }
}
