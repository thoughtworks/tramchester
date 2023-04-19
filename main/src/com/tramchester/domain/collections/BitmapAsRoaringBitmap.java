package com.tramchester.domain.collections;

import org.roaringbitmap.BatchIterator;
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
    public long cardinality() {
        return bitmap.getCardinality();
    }

    @Override
    public void clear() {
        bitmap.clear();
    }

    @Override
    public SimpleImmutableBitmap getSubmap(int start, int end) {
        final int bufferSize = 128;

        final int submapSize = end - start;
        final MutableRoaringBitmap submap = new MutableRoaringBitmap();
        final int[] buffer = new int[bufferSize];
        final int[] output = new int[bufferSize];

        BatchIterator batchIterator = bitmap.getBatchIterator();

        batchIterator.advanceIfNeeded(start);

        int value = -1;

        while (batchIterator.hasNext() && value<=end) {
            final int numInBatch = batchIterator.nextBatch(buffer);
            for (int i = 0; i < numInBatch; i++) {
                // todo maybe optimise by adding in batches via addN but need to make sure compute the offset value
                value = buffer[i];
                if (value>end) {
                    break;
                }
                if (value>=start) {

                    submap.add(value-start);
                }
            }
        }
        return new BitmapAsRoaringBitmap(submap, submapSize);
    }

    @Override
    public void set(int position) {
        bitmap.add(position);
    }

    @Override
    public void set(int[] positionsToSet) {
        bitmap.add(positionsToSet);
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
    public void or(SimpleImmutableBitmap other) {
        BitmapAsRoaringBitmap otherBitmap = (BitmapAsRoaringBitmap) other;
        bitmap.or(otherBitmap.bitmap);
    }

    @Override
    public void and(SimpleImmutableBitmap other) {
        BitmapAsRoaringBitmap otherBitmap = (BitmapAsRoaringBitmap) other;
        bitmap.and(otherBitmap.bitmap);
    }

    @Override
    public void andNot(SimpleImmutableBitmap other) {
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
