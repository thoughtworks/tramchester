package com.tramchester.domain;

import java.util.Collection;

public interface HasId {
    String getId();

    static <T extends HasId> String asIds(Collection<T> platforms) {
        StringBuilder ids = new StringBuilder();
        ids.append("[");
        platforms.forEach(platform -> ids.append(" '").append(platform.getId()).append("'"));
        ids.append("]");
        return ids.toString();
    }
}
