package com.tramchester.domain.presentation;

import com.tramchester.domain.places.Station;

public class StationNote extends Note {

    private Station station;

    public StationNote() {
        // deserialisation
        super();
    }

    public StationNote(NoteType noteType, String text, Station location) {
        super(text, noteType);
        this.station = location;
    }

    public Station getStation() {
        return station;
    }

    @Override
    public String toString() {
        return "StationNote{" +
                "station=" + station +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StationNote that = (StationNote) o;

        return getStation() != null ? getStation().equals(that.getStation()) : that.getStation() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getStation() != null ? getStation().hashCode() : 0);
        return result;
    }
}
