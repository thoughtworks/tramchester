package com.tramchester.domain.collections;

import org.roaringbitmap.BatchIterator;
import org.roaringbitmap.RoaringBatchIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.Objects;
import java.util.stream.Stream;

public class BitmapAsRoaringBitmap implements SimpleBitmap {
    private final RoaringBitmap bitmap;
    private final int size;

    public BitmapAsRoaringBitmap(int size) {
        this(new RoaringBitmap(), size);
    }

    private BitmapAsRoaringBitmap(RoaringBitmap source, int size) {
        this.bitmap = source;
        this.size = size;
    }

    public BitmapAsRoaringBitmap createCopy() {
        return new BitmapAsRoaringBitmap(bitmap.clone(), size);
    }

    @Override
    public long cardinality() {
        return bitmap.getCardinality();
    }

    @Override
    public Stream<Short> getBitIndexes() {
        return bitmap.stream().boxed().map(Integer::shortValue);
    }

    @Override
    public void clear() {
        bitmap.clear();
    }

    public BitmapAsRoaringBitmap copyRowAndColumn(int row, int column, int totalRows, int totalColumns) {
        final RoaringBitmap result = new RoaringBitmap();
        extractRow(result, row, totalColumns);
        extractColumn(result, column, totalRows, totalColumns);
        return new BitmapAsRoaringBitmap(result, size);
    }

    private void extractColumn(RoaringBitmap result, final int column, final int totalRows, final int totalColumns) {
        // used of buffer and addN has significant performance impact
        final int[] outputBuffer = new int[totalRows];
        int beginOfRow = 0;
        int index = 0;

        for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
            final int columnPosition = column + beginOfRow;
            if (bitmap.contains(columnPosition)) {
                outputBuffer[index++] = columnPosition;
            }
            beginOfRow = beginOfRow + totalColumns;
        }
        result.addN(outputBuffer, 0, index);
    }

    private void extractRow(RoaringBitmap result, final int row, final int totalColumns) {
        final int start =  row * totalColumns;
        final long end = start + totalColumns;

        final int[] readBuffer = new int[totalColumns];
        final int[] outputBuffer = new int[totalColumns];

        RoaringBatchIterator iterator = bitmap.getBatchIterator();
        iterator.advanceIfNeeded(start);
        final int read = iterator.nextBatch(readBuffer);

        int index = 0;
        while(index<read) {
            final int value = readBuffer[index];
            if (value>end) {
                break;
            }
            outputBuffer[index++] = value;
        }
        result.addN(outputBuffer, 0, index);
    }

    @Override
    public SimpleImmutableBitmap getSubmap(final int submapStart, final int submapEnd) {
        final int bufferSize = 128;
        final int[] readBuffer = new int[bufferSize];
        final int[] writeBuffer = new int[bufferSize];

        final RoaringBitmap submap = new RoaringBitmap();

        BatchIterator batchIterator = bitmap.getBatchIterator();
        batchIterator.advanceIfNeeded(submapStart);

        int readValue = -1;
        while (batchIterator.hasNext() && readValue<=submapEnd) {
            final int numInBatch = batchIterator.nextBatch(readBuffer);
            int index = 0;
            for (int i = 0; i < numInBatch; i++) {
                // todo maybe optimise by adding in batches via addN but need to make sure compute the offset value
                readValue = readBuffer[i];
                if (readValue>submapEnd) {
                    break;
                }
                if (readValue>=submapStart) {
                    writeBuffer[index++] = readValue-submapStart;
                    //submap.add(readValue-submapStart);
                }
            }
            submap.addN(writeBuffer, 0, index);
        }

        final int submapSize = (submapEnd - submapStart) + 1;

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

    public static BitmapAsRoaringBitmap and(BitmapAsRoaringBitmap bitmapA, BitmapAsRoaringBitmap bitmapB) {
        if (bitmapA.size!=bitmapB.size) {
            throw new RuntimeException("Size mismatch, got " + bitmapA.size + " and " + bitmapB.size);
        }
        RoaringBitmap result = RoaringBitmap.and(bitmapA.bitmap, bitmapB.bitmap);
        return new BitmapAsRoaringBitmap(result, bitmapA.size);
    }

    @Override
    public void andNot(SimpleImmutableBitmap other) {
        BitmapAsRoaringBitmap otherBitmap = (BitmapAsRoaringBitmap) other;
        bitmap.andNot(otherBitmap.bitmap);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BitmapAsRoaringBitmap bitmap1 = (BitmapAsRoaringBitmap) o;
        return size == bitmap1.size && bitmap.equals(bitmap1.bitmap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bitmap, size);
    }
}
