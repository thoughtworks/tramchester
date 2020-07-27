package com.tramchester.domain;

import com.tramchester.domain.places.IdForDTO;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface HasId<DOMAINTYPE extends GraphProperty> {
    IdFor<DOMAINTYPE> getId();

    static <T extends IdForDTO> String dtoAsIds(Collection<T> items) {
        return listToIdStringList(items, IdForDTO::forDTO);
    }

    static <T extends HasId<T> & GraphProperty> String asIds(Collection<T> items) {
        return listToIdStringList(items, item -> asId(item).toString());
    }

    @NotNull
    static <T> String listToIdStringList(Collection<T> items, GetsId<T> getsId) {
        StringBuilder ids = new StringBuilder();
        ids.append("[");
        items.forEach(item -> ids.append(" '").append(getsId.asString(item)).append("'"));
        ids.append("]");
        return ids.toString();
    }

    static <T extends HasId<T> & GraphProperty> IdFor<T> asId(T item) {
        return item.getId();
    }

    interface GetsId<T> {
        String asString(T item);
    }
}
