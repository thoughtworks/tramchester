package com.tramchester.domain.id;

import com.tramchester.domain.GraphProperty;
import com.tramchester.graph.GraphPropertyKey;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Entity;

public class StringIdFor<T extends GraphProperty> implements Comparable<StringIdFor<T>>, IdFor<T> {
    private final String theId;

    private StringIdFor(String theId) {
        this.theId = theId;
    }

    private StringIdFor() {
        this.theId = "";
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StringIdFor<?> idFor = (StringIdFor<?>) o;

        // interned strings, so == is ok here
        return theId.equals(idFor.theId);
    }

    @Override
    public String toString() {
        return "Id{'" + theId + "'}";
    }

    @Override
    public int hashCode() {
        return theId.hashCode();
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

    @Override
    public int compareTo(@NotNull StringIdFor<T> other) {
        return theId.compareTo(other.theId);
    }
}
