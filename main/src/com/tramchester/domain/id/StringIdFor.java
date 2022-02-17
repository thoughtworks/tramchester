package com.tramchester.domain.id;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.Route;
import com.tramchester.graph.GraphPropertyKey;
import org.neo4j.graphdb.Entity;

public class StringIdFor<T extends CoreDomain> implements IdFor<T> {
    private final String theId;
    private final int hashcode;

    protected StringIdFor(String theId) {
        this.theId = theId.intern();
        this.hashcode = theId.hashCode();
    }

    private StringIdFor() {
        this("");
    }

    public static <C extends CoreDomain> IdFor<C> createId(String text) {
        if (text==null) {
            return invalid();
        }
        if (text.isBlank()) {
            return invalid();
        }
        if (CompositeId.isComposite(text)) {
            return CompositeId.parse(text);
        }
        return new StringIdFor<>(text);
    }

    public static <CLASS extends CoreDomain> StringIdFor<CLASS> invalid() {
        return new StringIdFor<>();
    }

    public static <FROM extends CoreDomain,TO extends CoreDomain> IdFor<TO> convert(IdFor<FROM> original) {
        guardForType(original);
        StringIdFor<FROM> other = (StringIdFor<FROM>) original;
        return createId(other.theId);
    }

    public static <T extends CoreDomain, S extends CoreDomain> IdFor<T> withPrefix(String prefix, IdFor<S> original) {
        guardForType(original);
        StringIdFor<S> other = (StringIdFor<S>) original;
        return createId(prefix+other.theId);
    }

    public static <T extends CoreDomain, S extends CoreDomain> IdFor<Route> withSuffix(IdFor<S> original, String suffix) {
        guardForType(original);
        StringIdFor<S> other = (StringIdFor<S>) original;
        return createId(other.theId+suffix);
    }

    private static <FROM extends CoreDomain> void guardForType(IdFor<FROM> original) {
        if (!(original instanceof StringIdFor)) {
            throw new RuntimeException(original + " is not a StringIdFor");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        StringIdFor<?> otherId = (StringIdFor<?>) other;

        return theId.equals(otherId.theId);
    }

    @Override
    public String toString() {
        if (isValid()) {
            return "Id{'" + theId + "'}";
        } else {
            return "Id{NOT_VALID}";
        }
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    @Override
    public String forDTO() {
        return theId;
    }

    @Override
    public String getGraphId() {
        return theId;
    }

    @Override
    public boolean isValid() {
        return !theId.isEmpty();
    }

    public static <Z extends CoreDomain> IdFor<Z> getIdFromGraphEntity(Entity entity, GraphPropertyKey propertyKey) {
        String value =  entity.getProperty(propertyKey.getText()).toString();
        return createId(value);

    }

}
