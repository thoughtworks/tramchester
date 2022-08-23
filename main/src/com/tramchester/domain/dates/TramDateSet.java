package com.tramchester.domain.dates;

import com.tramchester.domain.CoreDomain;
import org.apache.commons.collections4.SetUtils;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class TramDateSet implements Iterable<TramDate> {
    private final Set<TramDate> dates;

    public TramDateSet() {
        dates = new HashSet<>();
    }

    public TramDateSet(TramDateSet other) {
        dates = new HashSet<>(other.dates);
    }

    public TramDateSet add(TramDate date) {
        dates.add(date);
        return this;
    }

    public boolean contains(final TramDate date) {
        return dates.contains(date);
    }

    public boolean isEmpty() {
        return dates.isEmpty();
    }

    public Stream<TramDate> stream() {
        return dates.stream();
    }

    public boolean removeAll(TramDateSet other) {
        return dates.removeAll(other.dates);
    }

    public TramDateSet addAll(TramDateSet other) {
        dates.addAll(other.dates);
        return this;
    }

    public static TramDateSet of(Set<LocalDate> dates) {
        TramDateSet result = new TramDateSet();
        result.addAll(dates.stream().map(TramDate::of).collect(collector()));
        return result;
    }

    public static <T extends CoreDomain> Collector<TramDate, TramDateSet, TramDateSet> collector() {
        return new Collector<>() {
            @Override
            public Supplier<TramDateSet> supplier() {
                return TramDateSet::new;
            }

            @Override
            public BiConsumer<TramDateSet, TramDate> accumulator() {
                return TramDateSet::add;
            }

            @Override
            public BinaryOperator<TramDateSet> combiner() {
                return TramDateSet::addAll;
            }

            @Override
            public Function<TramDateSet, TramDateSet> finisher() {
                return items -> items;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return SetUtils.unmodifiableSet(Characteristics.UNORDERED);
            }
        };
    }

    @NotNull
    @Override
    public Iterator<TramDate> iterator() {
        return dates.iterator();
    }

    @Override
    public void forEach(Consumer<? super TramDate> action) {
        dates.forEach(action);
    }

    @Override
    public String toString() {
        return "TramDateSet{" +
                "dates=" + dates +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TramDateSet tramDates = (TramDateSet) o;
        return dates.equals(tramDates.dates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dates);
    }
}
