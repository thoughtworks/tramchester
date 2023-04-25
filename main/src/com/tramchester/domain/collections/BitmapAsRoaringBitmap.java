package com.tramchester.domain.collections;

import org.roaringbitmap.*;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class BitmapAsRoaringBitmap implements SimpleBitmap {
    private final RoaringBitmap bitmap;
    private final int size;

    BitmapAsRoaringBitmap(int size) {
        this(new RoaringBitmap(), size);
    }

    private BitmapAsRoaringBitmap(RoaringBitmap source, int size) {
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
    public SimpleBitmap extractRowAndColumn(int row, int column, int totalRows, int totalColumns) {
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
            final int columnPosition = beginOfRow + column;
            if (bitmap.contains(columnPosition)) {
                //result.add(columnPosition);
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

        int i = 0;
        while(i<read) {
            final int value = readBuffer[i];
            if (value>end) {
                break;
            }
            outputBuffer[i++] = value;
        }
        result.addN(outputBuffer, 0, i);
    }

    @Override
    public SimpleImmutableBitmap getSubmap(int start, int end) {
        final int bufferSize = 128;

        final int submapSize = end - start;
        final RoaringBitmap submap = new RoaringBitmap();
        final int[] buffer = new int[bufferSize];

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
