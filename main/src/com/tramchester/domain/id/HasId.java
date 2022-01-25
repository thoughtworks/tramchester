package com.tramchester.domain.id;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.places.IdForDTO;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface HasId<DOMAINTYPE extends CoreDomain> {

    IdFor<DOMAINTYPE> getId();

    static <T extends IdForDTO> String dtoAsIds(Collection<T> items) {
        return listToIdStringList(items, IdForDTO::forDTO);
    }

    static <S extends  T, T extends HasId<T> & CoreDomain> String asIds(Collection<S> items) {
        return listToIdStringList(items, item -> item.getId().toString());
    }

    @NotNull
    static <T> String listToIdStringList(Collection<T> items, GetsId<T> getsId) {
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
