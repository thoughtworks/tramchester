package com.tramchester.domain.collections;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface SimpleBitmap {

    SimpleBitmap createCopy();

    int cardinality();

    void clear();

    SimpleBitmap getSubmap(int start, int end);

    void set(int position);

    void set(int position, boolean value);

    void setAll(int start, int end);

    void setAll(int start, int end, boolean value);

    boolean get(int position);

    void or(SimpleBitmap other);

    void and(SimpleBitmap other);

    void andNot(SimpleBitmap contained);

    IntStream stream();

    boolean isEmpty();

    int size();

    static int getPositionFor(int row, int column, int rows, int columns) {
        if (row >= rows) {
            throw new RuntimeException("Row is out of bounds, more than " + rows);
        }
        if (column >= columns) {
            throw new RuntimeException("Column is out of bounds, more than " + columns);

        }
        return (row * columns) + column;
    }

    static String displayAs(SimpleBitmap bitmap, int rows, int columns) {
        StringBuilder result = new StringBuilder();
        result.append(System.lineSeparator());
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                char bit = bitmap.get(SimpleBitmap.getPositionFor(row, column, rows, columns)) ? '1' : '0';
                result.append(bit);
            }
            result.append(System.lineSeparator());
        }
        return result.toString();
    }

    /***
     * factory method, so can swap bitmap impl's quickly
     * @param size size of the bit
     * @return an implementation of SimpleBitmap
     */
    static SimpleBitmap create(int size) {
        return new BitmapAsRoaringBitmap(size);
    }

    // test support
    static Stream<SimpleBitmap> getImplementationsOf(int size) {
        return Stream.of(new BitmapAsBitset(size), new BitmapAsRoaringBitmap(size));
    }
}
