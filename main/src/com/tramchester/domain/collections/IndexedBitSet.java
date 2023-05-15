package com.tramchester.domain.collections;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;

/***
 * Index-able bit map - set of N*M bits [....][....][....]
 */
public class IndexedBitSet implements ImmutableIndexedBitSet {
    private final int rows;
    private final int columns;
    private final BitmapAsRoaringBitmap bitmap;
    private final int totalSize;

    public static IndexedBitSet Square(int size) {
        return new IndexedBitSet(size, size);
    }

    private IndexedBitSet(int rows, int columns, BitmapAsRoaringBitmap bitmap) {
        this.rows = rows;
        this.columns = columns;
        totalSize = rows * columns;
        this.bitmap = bitmap;
    }

    public IndexedBitSet(int rows, int columns) {
        this(rows, columns, new BitmapAsRoaringBitmap(rows*columns));
    }

    public static IndexedBitSet getIdentity(int rows, int columns) {
        final IndexedBitSet result = new IndexedBitSet(rows, columns);

        result.bitmap.setAll(0, result.totalSize); // set bits to 1 from 0 to size
        return result;
    }


    /***
     * Set the bit at given row and column i.e. y'th column bit in row x
     * @param row the row to update
     * @param column the bit within the row to update
     */
    public void set(final int row, final int column) {
        final int position = getPositionFor(row, column);
        bitmap.set(position);
    }

    /***
     * Check if bit set
     * @param row row to query
     * @param column bit within row to check
     * @return true if column'th bit in row is set
     */
    public boolean isSet(final int row, final int column) {
        final int position = getPositionFor(row, column);
        return bitmap.get(position);
    }

    /***
     * Check if bit set
     * @return true if column'th bit in row is set
     */
    @Override
    public boolean isSet(final RouteIndexPair pair) {
        return isSet(pair.first(), pair.second());
    }

    /***
     * get the bits for one row
     * @param row rwo to return
     * @return bitset for that row
     */
    @Override
    public SimpleImmutableBitmap getBitSetForRow(final int row) {
        final int startPosition = getPositionFor(row, 0);
        final int endPositionExclusive = startPosition + columns; // plus num cols per row

        return bitmap.getSubmap(startPosition, endPositionExclusive-1);
    }

    /***
     * Directly insert a set of bits as a row
     * @param row the place to the bits
     * @param connectionsForRoute the bits for the row
     */
    public void insert(final int row, final SimpleBitmap connectionsForRoute) {
        final int startPosition = getPositionFor(row, 0);
        for (int column = 0; column < columns; column++) {
            bitmap.set(startPosition + column, connectionsForRoute.get(column));
        }
    }

    @Override
    public long numberOfBitsSet() {
        return bitmap.cardinality();
    }

    public void clear() {
        bitmap.clear();
    }

    /***
     * Apply a bitmask to one specific row via 'and'
     * @param row the row to apply the bitmask to
     * @param mask bitmask to use
     */
    public void applyAndToRow(final int row, final SimpleImmutableBitmap mask) {
        final int startPosition = getPositionFor(row, 0);

        // TODO more efficient ways to do this via a mask?
        for (int column = 0; column < columns; column++) {
            final int bitIndex = startPosition + column;
            final boolean andValue = bitmap.get(bitIndex) && mask.get(column);
            bitmap.set(bitIndex, andValue);
        }
    }

    public void or(SimpleImmutableBitmap immutableBitmap) {
        SimpleBitmap simpleBitmap = (SimpleBitmap) immutableBitmap;
        bitmap.or(simpleBitmap);
    }

    /***
     * position within the bitmap for row and column
     * @param row the row
     * @param column the bit within the row
     * @return absolute index into the bitset
     */
    private int getPositionFor(final int row, final int column) {
        if (row>=rows) {
            throw new RuntimeException("Row " + row + " is out of bounds, more than " + rows);
        }
        if (column>=columns) {
            throw new RuntimeException("Column" + column + " is out of bounds, more than " + columns);
        }
        return (row * columns) + column;
    }

    public Stream<Pair<Integer, Integer>> getPairs() {
        // note: range is inclusive
        return IntStream.range(0, rows ).boxed().
                flatMap(row -> getBitSetForRow(row).getBitIndexes().map(column -> Pair.of(row, column.intValue())));
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
    @Override
    public IndexedBitSet getCopyOfRowAndColumn(final int row, final int column) {
        final BitmapAsRoaringBitmap result = bitmap.copyRowAndColumn(row, column, rows, columns);
        return new IndexedBitSet(rows, columns, result);
    }

    public static IndexedBitSet and(ImmutableIndexedBitSet immutableA, ImmutableIndexedBitSet immutableB) {
        IndexedBitSet bitSetA = (IndexedBitSet) immutableA;
        IndexedBitSet bitSetB = (IndexedBitSet) immutableB;
        if (bitSetA.rows != bitSetB.rows) {
            throw new RuntimeException(format("Mismatch on matrix row size this %s other %s", bitSetA.rows, bitSetB.rows));
        }
        if (bitSetA.columns != bitSetB.columns) {
            throw new RuntimeException(format("Mismatch on matrix column size this %s other %s", bitSetA.columns, bitSetB.columns));
        }
        final BitmapAsRoaringBitmap and = BitmapAsRoaringBitmap.and(bitSetA.bitmap, bitSetB.bitmap);
        return new IndexedBitSet(bitSetA.rows, bitSetA.columns, and);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexedBitSet bitSet = (IndexedBitSet) o;
        return rows == bitSet.rows && columns == bitSet.columns && totalSize == bitSet.totalSize && bitmap.equals(bitSet.bitmap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rows, columns, bitmap, totalSize);
    }
}
