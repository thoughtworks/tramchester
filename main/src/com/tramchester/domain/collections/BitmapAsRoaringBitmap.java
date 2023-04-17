package com.tramchester.domain.collections;

import org.roaringbitmap.buffer.MutableRoaringBitmap;

import java.util.stream.IntStream;

import static org.roaringbitmap.buffer.MutableRoaringBitmap.add;

public class BitmapAsRoaringBitmap implements SimpleBitmap {
    private final MutableRoaringBitmap bitmap;
    private final int size;

    public BitmapAsRoaringBitmap(int size) {
        this(new MutableRoaringBitmap(), size);
    }

    public BitmapAsRoaringBitmap(MutableRoaringBitmap source, int size) {
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
        //MutableRoaringBitmap partial = bitmap.select(start, end);
        MutableRoaringBitmap partial = add(bitmap, start, end);

        return new BitmapAsRoaringBitmap(partial, end-start);
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
    public String displayAs(int rows, int columns) {
        StringBuilder result = new StringBuilder();
        result.append(System.lineSeparator());
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                char bit = bitmap.contains(getPositionFor(row, column, rows, columns)) ? '1' : '0';
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
