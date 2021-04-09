package com.tramchester.domain.id;

import com.tramchester.domain.GraphProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class CompositeId<DOMAINTYPE extends GraphProperty> implements IdFor<DOMAINTYPE> {

    private static final String DIVIDER = "_";

    private final IdSet<DOMAINTYPE> ids;

    public CompositeId(IdSet<DOMAINTYPE> ids) {
        this.ids = ids;
    }

    @SafeVarargs
    public CompositeId(IdFor<DOMAINTYPE> ...ids) {
        this.ids = new IdSet<>(Arrays.asList(ids));
    }

    public static <T extends HasId<T> & GraphProperty> CompositeId<T> parse(String text) {
        IdSet<T> ids = new IdSet<>();
        String[] parts = text.split(DIVIDER);
        for (String part : parts) {
            StringIdFor<T> id = StringIdFor.createId(part);
            ids.add(id);
        }
        return new CompositeId<T>(ids);
    }

    @Override
    public String forDTO() {
        StringBuilder result = new StringBuilder();
        getSorted().forEach(id -> {
            result.append(result.isEmpty() ? "" : DIVIDER);
            result.append(id.forDTO());
        });
        return result.toString();
    }

    @Override
    public String getGraphId() {
        StringBuilder result = new StringBuilder();
        getSorted().forEach(id -> {
            result.append(result.isEmpty() ? "" : DIVIDER);
            result.append(id.getGraphId());
        });
        return result.toString();
    }

    private List<IdFor<DOMAINTYPE>> getSorted() {
        return ids.stream().sorted().collect(Collectors.toList());
    }

    @Override
    public boolean isValid() {
        return ids.stream().map(IdFor::isValid).reduce((a, b) -> a && b).orElse(false);
    }

    @Override
    public String toString() {
        return "CompositeId{" +
                "ids=" + ids +
                '}';
    }

    public IdSet<DOMAINTYPE> getIds() {
        return ids;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompositeId<?> that = (CompositeId<?>) o;

        return ids.equals(that.ids);
    }

    @Override
    public int hashCode() {
        return ids.hashCode();
    }
}
