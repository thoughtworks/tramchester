package com.tramchester.domain.collections;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;
import java.util.stream.Stream;

/***
 * Simple Balanced Tree implementation with list semantics
 * Used to support route exploration in RouteToRouteCosts where a normal list implementation is orders of mag slower
 * @param <T>
 */
public class SimpleListItems<T> implements SimpleList<T> {

    private final Pair<SimpleList<T>, SimpleList<T>> tree;
    private final int size;

    SimpleListItems(SimpleList<T> left, SimpleList<T> right) {
        this.tree = Pair.of(left, right);
        size = tree.getLeft().size() + tree.getRight().size();
    }

    @Override
    public int size() {
        return size;
    }

    public Stream<T> stream() {
        return Stream.concat(tree.getLeft().stream(), tree.getRight().stream());
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public SimpleList<T> concat(SimpleList<T> other) {
        if (tree.getLeft().size() > tree.getRight().size()) {
            SimpleList<T> rhs = new SimpleListItems<>(tree.getRight(), other);
            return new SimpleListItems<>(tree.getLeft(), rhs);
        } else {
            return new SimpleListItems<>(this,  other);
        }
    }

    @Override
    public String toString() {
        return "SimpleListItems{" +
                "tree=" + tree +
                ", size=" + size +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleListItems<?> that = (SimpleListItems<?>) o;
        return size == that.size && tree.equals(that.tree);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tree, size);
    }
}
