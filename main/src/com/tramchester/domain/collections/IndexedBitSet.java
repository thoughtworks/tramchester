package com.tramchester.domain.collections;

import org.apache.commons.lang3.tuple.Pair;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;

/***
 * Index-able bit map - set of N*M bits [....][....][....]
 */
public class IndexedBitSet {
    private final int rows;
    private final int columns;
    private final SimpleBitmap bitmap;
    private final int totalSize;

    public static IndexedBitSet Square(int size) {
        return new IndexedBitSet(size, size);
    }

    private IndexedBitSet(int rows, int columns, SimpleBitmap bitmap) {
        this.rows = rows;
        this.columns = columns;
        totalSize = rows * columns;
        this.bitmap = bitmap;
    }

    public IndexedBitSet(int rows, int columns) {
        this(rows, columns, SimpleBitmap.create(rows*columns));
    }

    public static IndexedBitSet getIdentity(int rows, int columns) {
        IndexedBitSet result = new IndexedBitSet(rows, columns);

        result.bitmap.setAll(0, result.totalSize); // set bits to 1 from 0 to size
        return result;
    }

    public ImmutableBitSet createImmutable() {
        return new ImmutableBitSet(bitmap, getSize());
    }

    /***
     * Set the bit at given row and column i.e. y'th column bit in row x
     * @param row the row to update
     * @param column the bit within the row to update
     */
    public void set(int row, int column) {
        int position = getPositionFor(row, column);
        bitmap.set(position);
    }

    /***
     * Check if bit set
     * @param row row to query
     * @param column bit within row to check
     * @return true if column'th bit in row is set
     */
    public boolean isSet(int row, int column) {
        int position = getPositionFor(row, column);
        return bitmap.get(position);
    }

    /***
     * Check if bit set
     * @return true if column'th bit in row is set
     */
    public boolean isSet(RouteIndexPair pair) {
        return isSet(pair.first(), pair.second());
    }

    /***
     * get the bits for one row
     * @param row rwo to return
     * @return bitset for that row
     */
    public ImmutableBitSet getBitSetForRow(int row) {
        int startPosition = getPositionFor(row, 0);
        int endPosition = startPosition + columns; // plus num cols per row
        SimpleImmutableBitmap result = bitmap.getSubmap(startPosition, endPosition);

        return new ImmutableBitSet(result, endPosition-startPosition);
    }

    /***
     * Directly insert a set of bits as a row
     * @param row the place to the bits
     * @param connectionsForRoute the bits for the row
     */
    public void insert(int row, SimpleBitmap connectionsForRoute) {
        int startPosition = getPositionFor(row, 0);
        for (int column = 0; column < columns; column++) {
            bitmap.set(startPosition + column, connectionsForRoute.get(column));
        }
    }

    public long numberOfBitsSet() {
        return bitmap.cardinality();
    }

    public void clear() {
        bitmap.clear();
    }

    /***
     * Apply a bitmask to one specific row via 'and'
     * @param row the row to apply the bitmask to
     * @param bitMask bitmask to use
     */
    public void applyAndTo(int row, SimpleBitmap bitMask) {
        int startPosition = getPositionFor(row, 0);

        // TODO more efficient ways to do this via a mask?
        for (int i = 0; i < rows; i++) {
            int bitIndex = startPosition + i;
            boolean andValue = bitmap.get(bitIndex) && bitMask.get(i);
            bitmap.set(bitIndex, andValue);
        }
    }

    public void or(ImmutableBitSet other) {
        if (other.getSize() > getSize()) {
            throw new RuntimeException("Size mismatch, got " + other.getSize() + " but needed " + getSize());
        }
        bitmap.or(other.getContained());
    }

    private int getSize() {
        return rows*columns;
    }

    /***
     * position within the bitmap for row and column
     * @param row the row
     * @param column the bit within the row
     * @return absolute index into the bitset
     */
    private int getPositionFor(int row, int column) {
        if (row>rows) {
            throw new RuntimeException("Row " + row + " is out of bounds, more than " + rows);
        }
        if (column>columns) {
            throw new RuntimeException("Column" + column + " is out of bounds, more than " + columns);

        }
        return (row * columns) + column;
    }

    /***
     * And this bitset with the supplied one and return result as a new bitmap
     * @param other bitmap to 'and' this one with
     * @return a new bitmap
     */
    public IndexedBitSet and(IndexedBitSet other) {
        if (rows != other.rows) {
            throw new RuntimeException(format("Mismatch on matrix row size this %s other %s", rows, other.rows));
        }
        if (columns != other.columns) {
            throw new RuntimeException(format("Mismatch on matrix column size this %s other %s", columns, other.columns));
        }
//        SimpleBitmap cloned = bitmap.createCopy();
//        cloned.and(other.bitmap);
//        return new IndexedBitSet(rows, columns, cloned);
        return and(other.bitmap);
    }

    /***
     * And this bitset with the supplied one and return result as a new bitmap
     * @param other bitmap to 'and' this one with
     * @return a new bitmap
     */
    public IndexedBitSet and(SimpleBitmap other) {
//        SimpleBitmap cloned = this.bitmap.createCopy();
//        cloned.and(other);
//        return new IndexedBitSet(rows, columns, cloned);
        return new IndexedBitSet(rows, columns, bitmap.and(bitmap, other));
    }

    public Stream<Pair<Integer, Integer>> getPairs() {
        // note: range is inclusive
        return IntStream.range(0, rows ).boxed().
                flatMap(row -> getBitSetForRow(row).getBitsSet().boxed().map(column -> Pair.of(row, column)));
    }

    @Override
    public String toString() {
        return "IndexedBitSet{" +
                "rows=" + rows +
                ", columns=" + columns +
                ", totalSize=" + totalSize +
                ", bitSet=" + SimpleBitmap.displayAs(bitmap, rows, columns) +
                '}';
    }

    /***
     * Return a new grid with bits set if they were set in the row and column provided
     * @param row to select set bits from
     * @param column to select set bit from
     * @return IndexedBitSet of same dimensions
     */
    public IndexedBitSet getRowAndColumn(int row, int column) {
        SimpleBitmap result = bitmap.extractRowAndColumn(row, column, rows, columns);
        return new IndexedBitSet(rows, columns, result);
    }

    private SimpleBitmap createMaskFor(int row, int column) {
        final int size = rows * columns;
        SimpleBitmap result = SimpleBitmap.create(size);

        int rowStart = getPositionFor(row, 0);
        result.setAll(rowStart, rowStart+columns);

        int[] buffer = new int[rows];
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            int columnPosition = getPositionFor(rowIndex, column);
            buffer[rowIndex] = columnPosition;
            //result.set(columnPosition);
        }
        result.set(buffer);

        return result;

    }

}
