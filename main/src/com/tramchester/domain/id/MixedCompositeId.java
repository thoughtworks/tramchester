package com.tramchester.domain.id;

import com.tramchester.domain.GraphProperty;

import static java.lang.String.format;

public class MixedCompositeId<DOMAINTYPE extends GraphProperty, A extends GraphProperty, B extends GraphProperty>
        implements IdFor<DOMAINTYPE>{

    private static final String DIVIDER = "_";
    private final IdFor<A> idA;
    private final IdFor<B> idB;

    private MixedCompositeId(IdFor<A> idA, IdFor<B> idB) {
        this.idA = idA;
        this.idB = idB;
    }

    public static <T extends GraphProperty, A extends GraphProperty, B extends GraphProperty> IdFor<T> createId(IdFor<A> itemA, IdFor<B> itemB) {
        if (itemA.forDTO().contains(DIVIDER) || itemB.forDTO().contains(DIVIDER)) {
            String message = format("Component ids must not contain divider idA:'%s' idB:'%s'", itemA, itemB);
            throw new RuntimeException(message);
        }
        return new MixedCompositeId<>(itemA, itemB);
    }

    public static <T extends GraphProperty, AAA extends HasId<AAA>, BBB extends HasId<BBB>> IdFor<T> parse(String value) {
        int indexOf = value.indexOf(DIVIDER);
        String partA = value.substring(0, indexOf);
        String partB = value.substring(indexOf+1);
        return new MixedCompositeId<T,AAA,BBB>(StringIdFor.createId(partA), StringIdFor.createId(partB));
    }

    @Override
    public String forDTO() {
        return idA.forDTO()+DIVIDER+idB.forDTO();
    }

    @Override
    public String getGraphId() {
        return idA.getGraphId()+DIVIDER+idB.getGraphId();
    }

    @Override
    public boolean isValid() {
        return idA.isValid() && idB.isValid();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MixedCompositeId<?, ?, ?> that = (MixedCompositeId<?, ?, ?>) o;

        if (!idA.equals(that.idA)) return false;
        return idB.equals(that.idB);
    }

    @Override
    public int hashCode() {
        int result = idA.hashCode();
        result = 31 * result + idB.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MixedCompositeId{" + idA + "," + idB + '}';
    }
}
