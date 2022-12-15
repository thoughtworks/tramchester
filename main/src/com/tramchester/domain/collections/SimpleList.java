package com.tramchester.domain.collections;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

public interface SimpleList<T> {

    static <T> SimpleList<T> concat(SimpleList<T> listA, SimpleList<T> listB) {
        return listA.concat(listB);
    }

    static <T> Collector<T, ArrayList<T>, SimpleList<T>> collector() {
        return new Collector<>() {
            @Override
            public Supplier<ArrayList<T>> supplier() {
                return ArrayList::new;
            }

            @Override
            public BiConsumer<ArrayList<T>, T> accumulator() {
                return List::add;
            }

            @Override
            public BinaryOperator<ArrayList<T>> combiner() {
                return (listA, listB) -> {
                    ArrayList<T> result = new ArrayList<>(listA);
                    result.addAll(listB);
                    return result;
                };
            }

            @Override
            public Function<ArrayList<T>, SimpleList<T>> finisher() {
                return SimpleListItems::new;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return EnumSet.noneOf(Characteristics.class);
            }
        };

    }

    int size();

    Stream<T> stream();

    boolean isSingleton();

    SimpleList<T> concat(SimpleList<T> other);

    boolean isEmpty();

    List<T> toList();
}
