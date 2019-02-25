package com.tramchester.domain.presentation;

public class ProximityGroup {

    public static final ProximityGroup MY_LOCATION = new ProximityGroup(1,"Nearby");
    public static final ProximityGroup RECENT = new ProximityGroup(2,"Recent");
    public static final ProximityGroup NEAREST_STOPS = new ProximityGroup(3,"Nearest Stops");
    public static final ProximityGroup ALL = new ProximityGroup(4,"All Stops");

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
