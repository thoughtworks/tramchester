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
    private final BitmapImpl bitSet;
    private final int totalSize;

    public static IndexedBitSet Square(int size) {
        return new IndexedBitSet(size, size);
    }

    private IndexedBitSet(int rows, int columns, BitmapImpl bitSet) {
        this.rows = rows;
        this.columns = columns;
        totalSize = rows * columns;
        this.bitSet = bitSet;
    }

    public IndexedBitSet(int rows, int columns) {
        this(rows, columns, new BitmapImpl(rows * columns));
    }

    public static IndexedBitSet getIdentity(int rows, int columns) {
        IndexedBitSet result = new IndexedBitSet(rows, columns);

        result.bitSet.setAll(0, result.totalSize); // set bits to 1 from 0 to size
        return result;
    }

    public ImmutableBitSet createImmutable() {
        return new ImmutableBitSet(bitSet, getSize());
    }

    /***
     * Set the bit at given row and column i.e. y'th column bit in row x
     * @param row the row to update
     * @param column the bit within the row to update
     */
    public void set(int row, int column) {
        int position = getPositionFor(row, column);
        bitSet.set(position);
    }

    /***
     * Check if bit set
     * @param row row to query
     * @param column bit within row to check
     * @return true if column'th bit in row is set
     */
    public boolean isSet(int row, int column) {
        int position = getPositionFor(row, column);
        return bitSet.get(position);
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
        BitmapImpl result = bitSet.getSubmap(startPosition, endPosition);

        return new ImmutableBitSet(result, endPosition-startPosition);
    }

    /***
     * Directly insert a set of bits as a row
     * @param row the place to the bits
     * @param connectionsForRoute the bits for the row
     */
    public void insert(int row, BitmapImpl connectionsForRoute) {
        int startPosition = getPositionFor(row, 0);
        for (int column = 0; column < columns; column++) {
            bitSet.set(startPosition + column, connectionsForRoute.get(column));
        }
    }

    public int numberOfBitsSet() {
        return bitSet.cardinality();
    }

    public void clear() {
        bitSet.clear();
    }

    /***
     * Apply a bitmask to one specific row via 'and'
     * @param row the row to apply the bitmask to
     * @param bitMask bitmask to use
     */
    public void applyAndTo(int row, BitmapImpl bitMask) {
        int startPosition = getPositionFor(row, 0);

        // TODO more efficient ways to do this via a mask?
        for (int i = 0; i < rows; i++) {
            int bitIndex = startPosition + i;
            boolean andValue = bitSet.get(bitIndex) && bitMask.get(i);
            bitSet.set(bitIndex, andValue);
        }
    }

    public void or(ImmutableBitSet other) {
        if (other.getSize() > getSize()) {
            throw new RuntimeException("Size mismatch, got " + other.getSize() + " but needed " + getSize());
        }
        bitSet.or(other.getContained());
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
        if (row>=rows) {
            throw new RuntimeException("Row is out of bounds, more than " + rows);
        }
        if (column>=columns) {
            throw new RuntimeException("Column is out of bounds, more than " + columns);

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
        BitmapImpl cloned = bitSet.createCopy();
        cloned.and(other.bitSet);
        return new IndexedBitSet(rows, columns, cloned);
    }

    /***
     * And this bitset with the supplied one and return result as a new bitmap
     * @param other bitmap to 'and' this one with
     * @return a new bitmap
     */
    public IndexedBitSet and(BitmapImpl other) {
        //BitSet cloned = (BitSet) this.bitSet.clone();
        BitmapImpl cloned = this.bitSet.createCopy();
        cloned.and(other);
        return new IndexedBitSet(rows, columns, cloned);
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
                ", bitSet=" + bitSet.display(rows, columns) +
                '}';
    }

    /***
     * Return a new grid with bits set if they were set in the row and column provided
     * @param row to select set bits from
     * @param column to select set bit from
     * @return IndexedBitSet of same dimensions
     */
    public IndexedBitSet getRowAndColumn(int row, int column) {
        BitmapImpl mask = createMaskFor(row, column);
        //BitSet clone = (BitSet) this.bitSet.clone();
        BitmapImpl clone = this.bitSet.createCopy();
        clone.and(mask);
        return new IndexedBitSet(rows, columns, clone);
    }

    private BitmapImpl createMaskFor(int row, int column) {
        BitmapImpl result = new BitmapImpl(rows * columns);

        int rowStart = getPositionFor(row, 0);
        result.setAll(rowStart, rowStart+columns, true);

        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            int columnPosition = getPositionFor(rowIndex, column);
            result.set(columnPosition);
        }

        return result;

    }

}
