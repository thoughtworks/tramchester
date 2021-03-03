package com.tramchester.domain.presentation.DTO;

public class StationLinkDTO {
    private StationRefWithPosition begin;
    private StationRefWithPosition end;

    public StationLinkDTO(StationRefWithPosition begin, StationRefWithPosition end) {
        this.begin = begin;
        this.end = end;
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

    public void setEnd(StationRefWithPosition end) {
        this.end = end;
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
}
