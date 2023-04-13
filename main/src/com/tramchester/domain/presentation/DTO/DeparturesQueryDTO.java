package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.Nulls;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.reference.TransportMode;

import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

@JsonTypeName("DeparturesQuery")
public class DeparturesQueryDTO {

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("time")
    private LocalTime time;

    @JsonProperty("locationType")
    private LocationType locationType;

    @JsonProperty("locationId")
    private IdForDTO locationId;

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("modes")
    private Set<TransportMode> modes;

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("notes")
    private Boolean includeNotes;

    public DeparturesQueryDTO(LocationType locationType, IdForDTO locationId, boolean includeNotes) {
        this.locationType = locationType;
        this.locationId = locationId;
        this.includeNotes = includeNotes;
        modes = Collections.emptySet();
        // deserialisation
    }

    public DeparturesQueryDTO() {
        modes = Collections.emptySet();
        includeNotes = false;
        time = LocalTime.MAX;
        // deserialisation
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public IdForDTO getLocationId() {
        return locationId;
    }

    public EnumSet<TransportMode> getModes() {
        if (modes.isEmpty()) {
            return EnumSet.noneOf(TransportMode.class);
        }
        return EnumSet.copyOf(modes);
    }

    public void setModes(Set<TransportMode> modes) {
        this.modes = modes;
    }

    public boolean getIncludeNotes() {
        return includeNotes;
    }

    public void setIncludeNotes(boolean includeNotes) {
        this.includeNotes = includeNotes;
    }

    @Override
    public String toString() {
        return "DeparturesQueryDTO{" +
                "time=" + time +
                ", locationType=" + locationType +
                ", locationId='" + locationId + '\'' +
                ", modes=" + modes +
                ", includeNotes=" + includeNotes +
                '}';
    }

    public boolean hasValidTime() {
        return time != LocalTime.MAX;
    }
}
