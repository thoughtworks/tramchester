package com.tramchester.domain.collections;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    public SimpleListItems(List<T> list) {
        size = list.size();
        SimpleList<T> left = new SimpleListEmpty<>();

        Optional<SimpleList<T>> maybeRight = list.stream().map(this::create).reduce((listA, listB) -> listA.concat(listB));

        SimpleList<T> right = maybeRight.orElse(new SimpleListEmpty<>());

        tree = Pair.of(left, right);
    }

    private SimpleList<T> create(T item) {
        return new SimpleListSingleton<>(item);
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
    public boolean isEmpty() {
        return tree.getLeft().isEmpty() && tree.getLeft().isEmpty();
    }

    @Override
    public List<T> toList() {
        List<T> result = new ArrayList<T>(tree.getLeft().toList());
        result.addAll(tree.getRight().toList());
        return result;
    }

    @Override
    public String toString() {
        return "[" + tree.getLeft().toString() + "," + tree.getRight().toString() + "]";
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
