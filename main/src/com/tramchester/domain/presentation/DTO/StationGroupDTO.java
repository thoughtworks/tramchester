package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.places.CompositeStation;

import java.util.List;
import java.util.stream.Collectors;

public class StationGroupDTO {
    private StationRefWithPosition parent;
    private List<StationRefWithPosition> contained;

    public StationGroupDTO() {
        // deserialization
    }

    public StationGroupDTO(StationRefWithPosition parent, List<StationRefWithPosition> contained) {
        this.parent = parent;
        this.contained = contained;
    }


    public StationRefWithPosition getParent() {
        return parent;
    }

    public List<StationRefWithPosition> getContained() {
        return contained;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StationGroupDTO that = (StationGroupDTO) o;

        if (parent != null ? !parent.equals(that.parent) : that.parent != null) return false;
        return contained != null ? contained.equals(that.contained) : that.contained == null;
    }

    @Override
    public int hashCode() {
        int result = parent != null ? parent.hashCode() : 0;
        result = 31 * result + (contained != null ? contained.hashCode() : 0);
        return result;
    }

    public static StationGroupDTO create(CompositeStation compositeStation) {
        StationRefWithPosition parent = new StationRefWithPosition(compositeStation);
        List<StationRefWithPosition> contained = compositeStation.getContained().stream().
                map(StationRefWithPosition::new).collect(Collectors.toList());
        return new StationGroupDTO(parent, contained);
    }
}
