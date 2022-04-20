package com.tramchester.domain.places;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.HasGridPosition;
import com.tramchester.repository.naptan.NaptanStopAreaType;

public class NaptanArea implements HasGridPosition, HasId<NaptanArea>, CoreDomain {
    private final IdFor<NaptanArea> id;
    private final String name;
    private final GridPosition gridPosition;
    private final boolean active;
    private final NaptanStopAreaType type;

    public NaptanArea(IdFor<NaptanArea> id, String name, GridPosition gridPosition, boolean active, NaptanStopAreaType type) {
        this.id = id;
        this.name = name;
        this.gridPosition = gridPosition;
        this.active = active;
        this.type = type;
    }

    public static IdFor<NaptanArea> createId(String text) {
        return StringIdFor.createId(text);
    }

    public static NaptanArea Invalid() {
        return new NaptanArea(StringIdFor.createId("Invalid"), "Invalid", GridPosition.Invalid, false,
                NaptanStopAreaType.Unknown);
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
                ", type=" + type +
                '}';
    }

    public boolean isActive() {
        return active;
    }

    public NaptanStopAreaType getType() {
        return type;
    }
}
