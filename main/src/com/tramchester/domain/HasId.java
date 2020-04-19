package com.tramchester.domain;

import java.util.Collection;

public interface HasId {
    String getId();

    static <T extends HasId> String asIds(Collection<T> items) {
        StringBuilder ids = new StringBuilder();
        ids.append("[");
        items.forEach(platform -> ids.append(" '").append(platform.getId()).append("'"));
        ids.append("]");
        return ids.toString();
    }

    static <T extends HasId> String asId(T item) {
        return item.getId();
    }
}
