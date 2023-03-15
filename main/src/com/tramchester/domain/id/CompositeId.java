package com.tramchester.domain.id;

import com.tramchester.domain.CoreDomain;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CompositeId<T extends CoreDomain> implements IdFor<T> {

    private static final char BEGIN = '[';
    private static final char END = ']';
    private static final String DIVIDER = "_";

    private final IdSet<T> ids;

    private CompositeId(IdSet<T> ids) {
        this.ids = ids;
    }

    @SafeVarargs
    private CompositeId(IdFor<T> ...ids) {
        this.ids = new IdSet<>(Arrays.asList(ids));
    }

    // <T extends HasId<T> & GraphProperty>
    private static <T extends CoreDomain> CompositeId<T> parse(String text, Class<T> domainType) {
        if (!isComposite(text)) {
            throw new RuntimeException("Could not parse " + text);
        }

        String bare = text.substring(1, text.length() - 1);
        IdSet<T> ids = new IdSet<>();
        String[] parts = bare.split(DIVIDER);
        for (String part : parts) {
            IdFor<T> id = StringIdFor.createId(part, domainType);
            ids.add(id);
        }
        return new CompositeId<>(ids);
    }

    public static boolean isComposite(String text) {
        int length = text.length();
        if (length < 2) {
            return false;
        }
        return (text.charAt(0)==BEGIN && text.charAt(length -1)==END);
    }

    @Override
    public String forDTO() {
        return serialize(IdFor::forDTO);
    }

    @Override
    public String getGraphId() {
        return serialize(IdFor::getGraphId);
    }

    private String serialize(IdToString map) {
        StringBuilder result = new StringBuilder();
        result.append(BEGIN);
        getSorted().forEach(id -> {
            result.append(result.length()==1 ? "" : DIVIDER);
            result.append(map.asString(id));
        });
        result.append(END);
        return result.toString();
    }

    private interface IdToString {
        String asString(IdFor<?> id);
    }

    private List<IdFor<T>> getSorted() {
        return ids.stream().sorted().collect(Collectors.toList());
    }

    @Override
    public boolean isValid() {
        return ids.stream().map(IdFor::isValid).reduce((a, b) -> a && b).orElse(false);
    }

    @Override
    public Class<T> getDomainType() {
        return null;
    }

    @Override
    public String toString() {
        return "CompositeId{" +
                "ids=" + ids +
                '}';
    }

    public IdSet<T> getIds() {
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
