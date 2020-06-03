package com.tramchester.domain.presentation;

import java.util.Arrays;
import java.util.List;

public class ProximityGroup {

    private int order;
    private String name;

    public ProximityGroup() {
        // deserialisation
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProximityGroup that = (ProximityGroup) o;

        if (order != that.order) return false;
        return name != null ? name.equals(that.name) : that.name == null;

    }

    @Override
    public int hashCode() {
        int result = order;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public ProximityGroup(int order, String name) {

        this.order = order;
        this.name = name;
    }

    public int getOrder() {
        return order;
    }

    public String getName() {
        return name;
    }
}
