package com.tramchester.domain.collections;

import java.util.BitSet;

import static java.lang.String.format;

/***
 * Indexable square bit map - set of N*N rows of bits [....][....][....]
 */
public class IndexedBitSet {
    private final int numberOfRows;
    private final BitSet bitSet;
    private final int totalSize;

    public IndexedBitSet(int numberOfRows) {
        this.numberOfRows = numberOfRows;
        totalSize = numberOfRows * numberOfRows;
        bitSet = new BitSet(totalSize);
    }

    private IndexedBitSet(int numberOfRows, BitSet bitSet) {
        this.numberOfRows = numberOfRows;
        totalSize = numberOfRows * numberOfRows;
        this.bitSet = bitSet;
    }

    public static IndexedBitSet getIdentity(int numberOfRows) {
        IndexedBitSet result = new IndexedBitSet(numberOfRows);

        result.bitSet.set(0, result.totalSize); // set bits to 1 from 0 to size
        return result;
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
     * get the bits for one row
     * @param row rwo to return
     * @return bitset for that row
     */
    public ImmutableBitSet getBitSetForRow(int row) {
        int startPosition = getPositionFor(row, 0);
        int endPosition = startPosition + numberOfRows;
        BitSet result = bitSet.get(startPosition, endPosition);

        return new ImmutableBitSet(result);
    }

    /***
     * Directly insert a set of bits as a row
     * @param row the place to the bits
     * @param connectionsForRoute the bits for the row
     */
    public void insert(int row, BitSet connectionsForRoute) {
        int startPosition = getPositionFor(row, 0);
        for (int i = 0; i < numberOfRows; i++) {
            bitSet.set(startPosition + i, connectionsForRoute.get(i));
        }
    }

    public int numberOfBitsSet() {
        return bitSet.cardinality();
    }

    public void clear() {
        bitSet.clear();
    }

    /***
     * Apply a bitmask to one sepcific row via 'and'
     * @param row the row to apply the bitmask to
     * @param bitMask bitmask to use
     */
    public void applyAndTo(int row, BitSet bitMask) {
        int startPosition = getPositionFor(row, 0);

        // TODO more efficient ways to do this via a mask?
        for (int i = 0; i < numberOfRows; i++) {
            int bitIndex = startPosition + i;
            boolean andValue = this.bitSet.get(bitIndex) && bitMask.get(i);
            this.bitSet.set(bitIndex, andValue);
        }
    }

    /***
     * poisition within the bitmap for row and column
     * @param row the row
     * @param column the bit within the row
     * @return absolute index into the bitset
     */
    private int getPositionFor(int row, int column) {
        return (row * numberOfRows) + column;
    }

    /***
     * And this bitset with the supplied one and return result as a new bitmap
     * @param other bitmap to 'and' this one with
     * @return a new bitmap
     */
    public IndexedBitSet and(IndexedBitSet other) {
        if (numberOfRows != other.numberOfRows) {
            throw new RuntimeException(format("Mismatch on matrix size this %s other %s", numberOfRows, other.numberOfRows));
        }
        BitSet cloned = (BitSet) this.bitSet.clone();
        cloned.and(other.bitSet);
        return new IndexedBitSet(numberOfRows, bitSet);
    }

    @Override
    public String toString() {
        return "IndexedBitSet{" +
                "numberOfRoutes=" + numberOfRows +
                ", bitSet=" + bitSet.toString() +
                ", totalSize=" + totalSize +
                '}';
    }
}
