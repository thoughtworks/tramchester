package com.tramchester.domain.id;

import com.tramchester.domain.GraphProperty;
import com.tramchester.graph.GraphPropertyKey;
import org.neo4j.graphdb.Entity;

public class StringIdFor<T extends GraphProperty> implements IdFor<T> {
    private final String theId;
    private final int hashcode;

    protected StringIdFor(String theId) {
        this.theId = theId.intern();
        this.hashcode = theId.hashCode();
    }

    private StringIdFor() {
        this("");
    }

    public static <C extends GraphProperty> IdFor<C> createId(String text) {
        if (CompositeId.isComposite(text)) {
            return CompositeId.parse(text);
        }
        return new StringIdFor<>(text);
    }

    public static <CLASS extends GraphProperty> StringIdFor<CLASS> invalid() {
        return new StringIdFor<>();
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
        return "Id{'" + theId + "'}";
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

    public static <Z extends GraphProperty> IdFor<Z> getIdFromGraphEntity(Entity entity, GraphPropertyKey propertyKey) {
        String value =  entity.getProperty(propertyKey.getText()).toString();
        return createId(value);

    }

    public static <Z extends GraphProperty> IdFor<Z> getCompositeIdFromGraphEntity(Entity entity, GraphPropertyKey propertyKey) {
        String value = entity.getProperty(propertyKey.getText()).toString();
        return MixedCompositeId.parse(value);
    }

}
