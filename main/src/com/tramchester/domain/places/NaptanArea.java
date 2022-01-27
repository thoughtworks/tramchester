package com.tramchester.domain.places;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.HasGridPosition;

public class NaptanArea implements HasGridPosition, HasId<NaptanArea>, CoreDomain {
    private final IdFor<NaptanArea> id;
    private final String name;
    private final GridPosition gridPosition;
    private final boolean active;

    public NaptanArea(IdFor<NaptanArea> id, String name, GridPosition gridPosition, boolean active) {
        this.id = id;
        this.name = name;
        this.gridPosition = gridPosition;
        this.active = active;
    }

    public static IdFor<NaptanArea> createId(String text) {
        return StringIdFor.createId(text);
    }

    public String getName() {
        return name;
    }

    @Override
    public GridPosition getGridPosition() {
        return gridPosition;
    }

    @Override
    public IdFor<NaptanArea> getId() {
        return id;
    }

    @Override
    public String toString() {
        return "NaptanArea{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", gridPosition=" + gridPosition +
                ", active=" + active +
                '}';
    }

    public boolean isActive() {
        return active;
    }
}
