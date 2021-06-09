package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.StationLink;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.reference.TransportMode;

import java.util.Set;

public class StationLinkDTO {
    private StationRefWithPosition begin;
    private StationRefWithPosition end;
    private Set<TransportMode> transportModes;

    public StationLinkDTO(StationRefWithPosition begin, StationRefWithPosition end, Set<TransportMode> transportModes) {
        this.begin = begin;
        this.end = end;
        this.transportModes = transportModes;
    }

    public StationLinkDTO() {
        // deserialisation
    }

    public StationRefWithPosition getBegin() {
        return begin;
    }

    public StationRefWithPosition getEnd() {
        return end;
    }

    public Set<TransportMode> getTransportModes() {
        return transportModes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StationLinkDTO that = (StationLinkDTO) o;

        if (!begin.equals(that.begin)) return false;
        return end.equals(that.end);
    }

    @Override
    public int hashCode() {
        int result = begin.hashCode();
        result = 31 * result + end.hashCode();
        return result;
    }

    public static StationLinkDTO create(StationLink stationLink) {
        StationPair stationPair = stationLink.getStations();
        StationRefWithPosition begin = new StationRefWithPosition(stationPair.getBegin());
        StationRefWithPosition end = new StationRefWithPosition(stationPair.getEnd());
        return new StationLinkDTO(begin, end, stationLink.getModes());
    }
}
