package com.tramchester.domain.id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphPropertyKey;
import org.neo4j.graphdb.Entity;

import java.util.Objects;
import java.util.Set;

@JsonDeserialize(using = StringIdForDeserializer.class)
@JsonSerialize(using = StringIdForSerializer.class)
public class StringIdFor<T extends CoreDomain> implements IdFor<T> {
    private final String theId;
    private final int hashcode;
    private final Class<T> domainType;

    protected StringIdFor(String theId, Class<T> domainType) {
        this.theId = theId.intern();
        this.domainType = domainType;
        this.hashcode = Objects.hash(theId, domainType);
    }

    private StringIdFor(Class<T> domainType) {
        this("", domainType);
    }

    // todo package private?
    public static <C extends CoreDomain> IdFor<C> createId(String text, Class<C> domainType) {
        if (text==null) {
            return invalid(domainType);
        }
        if (text.isBlank()) {
            return invalid(domainType);
        }
//        if (CompositeId.isComposite(text)) {
//            return CompositeId.parse(text, domainType);
//        }
        return new StringIdFor<>(text, domainType);
    }

    public static IdFor<Station> createId(IdForDTO idForDTO, Class<Station> klass) {
        return createId(idForDTO.getActualId(), klass);
    }

    public static <T extends CoreDomain> IdSet<T> createIds(Set<String> items, Class<T> domainClass) {
        return items.stream().map(item -> StringIdFor.createId(item, domainClass)).collect(IdSet.idCollector());
    }

    String getContainedId() {
        return theId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringIdFor<?> that = (StringIdFor<?>) o;
        return theId.equals(that.theId) && domainType.equals(that.domainType);
    }


//    @Override
//    public boolean equals(Object other) {
//        if (this == other) return true;
//        if (other == null || getClass() != other.getClass()) return false;
//
//        StringIdFor<?> otherId = (StringIdFor<?>) other;
//
//        return theId.equals(otherId.theId);
//    }

    @Override
    public String toString() {
        String domainName = domainType.getSimpleName();
        if (isValid()) {
            return "Id{'" + domainName+ ":" + theId + "'}";
        } else {
            return "Id{"+domainName+":NOT_VALID}";
        }
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    @Override
    public String getGraphId() {
        return theId;
    }

    @Override
    public boolean isValid() {
        return !theId.isEmpty();
    }

    @Override
    public Class<T> getDomainType() {
        return domainType;
    }

    public static <Z extends CoreDomain> IdFor<Z> getIdFromGraphEntity(Entity entity, GraphPropertyKey propertyKey, Class<Z> domainType) {
        String value =  entity.getProperty(propertyKey.getText()).toString();
        return createId(value, domainType);
    }

    public static <CLASS extends CoreDomain> StringIdFor<CLASS> invalid(Class<CLASS> domainType) {
        return new StringIdFor<>(domainType);
    }

    public static <FROM extends CoreDomain,TO extends CoreDomain> IdFor<TO> convert(IdFor<FROM> original, Class<TO> domainType) {
        guardForType(original);
        StringIdFor<FROM> other = (StringIdFor<FROM>) original;
        return createId(other.theId, domainType);
    }

    public static <T extends CoreDomain, S extends CoreDomain> IdFor<T> withPrefix(String prefix, IdFor<S> original, Class<T> domainType) {
        guardForType(original);
        StringIdFor<S> other = (StringIdFor<S>) original;
        return createId(prefix+other.theId, domainType);
    }

    private static <FROM extends CoreDomain> void guardForType(IdFor<FROM> original) {
        if (!(original instanceof StringIdFor)) {
            throw new RuntimeException(original + " is not a StringIdFor");
        }
    }

}
