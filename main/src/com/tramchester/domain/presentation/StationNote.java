package com.tramchester.domain.presentation;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.StationRefDTO;

public class StationNote extends Note {

    private StationRefDTO stationRef;

    public StationNote() {
        // deserialisation
        super();
    }

    public StationNote(NoteType noteType, String text, Station location) {
        super(text, noteType);
        this.stationRef = new StationRefDTO(location);
    }

    @SuppressWarnings("WeakerAccess")
    public StationRefDTO getStationRef() {
        return stationRef;
    }

    @Override
    public String toString() {
        return "StationNote{" +
                "station=" + stationRef +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StationNote that = (StationNote) o;

        return getStationRef() != null ? getStationRef().equals(that.getStationRef()) : that.getStationRef() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getStationRef() != null ? getStationRef().hashCode() : 0);
        return result;
    }
}
