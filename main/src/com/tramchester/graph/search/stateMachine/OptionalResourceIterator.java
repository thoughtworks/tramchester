package com.tramchester.graph.search.stateMachine;

import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.internal.helpers.collection.Iterables;

import java.util.Collection;

public class OptionalResourceIterator<T> implements ResourceIterable<T> {

    private final ResourceIterable<T> contained;
    private final boolean empty;

    public static <T> OptionalResourceIterator<T> from(Collection<T> collection) {
        return new OptionalResourceIterator<>(collection);
    }

    public OptionalResourceIterator(Collection<T> collection) {
        empty = collection.isEmpty();
        contained = Iterables.asResourceIterable(collection);

    }

    @Override
    public ResourceIterator<T> iterator() {
        return contained.iterator();
    }

    @Override
    public void close() {
        contained.close();
    }

    public boolean isEmpty() {
        return empty;
    }
}
