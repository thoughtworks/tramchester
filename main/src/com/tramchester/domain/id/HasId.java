package com.tramchester.domain.id;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.collections.DomainPair;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface HasId<DOMAINTYPE extends CoreDomain> {

    IdFor<DOMAINTYPE> getId();

    static <S extends HasId<?>> String asIds(Collection<S> items) {
        return collectionToIdStringList(items, item -> item.getId().toString());
    }

    static <P extends DomainPair<?>> String asIds(P pair) {
        return "(" + pair.first().getId() + ", " + pair.second().getId() + ")";
    }

//    static String asIdsNested(List<List<RoutePair>> listOfList) {
//        Stream<String> stream = listOfList.stream().map(HasId::pairsAsIds);
//        return stream.reduce((a,b) -> a+", "+b).orElse("");
//    }

//    static <P extends DomainPair<?>> String pairsAsIds(Collection<P> items) {
//        return collectionToIdStringList(items, HasId::asIds);
//    }

    static String asIds(IdMap<?> idMap) {
        return idMap.getIds().toString();
    }

    static String asIds(LocationSet locationSet) {
        return locationSet.asIds();
    }

    @NotNull
    static <T> String collectionToIdStringList(Collection<T> items, GetsId<T> getsId) {
        StringBuilder ids = new StringBuilder();
        ids.append("[");
        items.forEach(item -> ids.append(" '").append(getsId.asString(item)).append("'"));
        ids.append("]");
        return ids.toString();
    }

    static <T extends HasId<T> & CoreDomain> IdFor<T> asId(T item) {
        return item.getId();
    }

    interface GetsId<T> {
        String asString(T item);
    }
}
