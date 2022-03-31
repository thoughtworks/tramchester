package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.Nulls;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.reference.TransportMode;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Set;

@JsonTypeName("DeparturesQuery")
public class DeparturesQueryDTO {
    @JsonProperty("time")
    private LocalTime time;

    @JsonProperty("startType")
    private LocationType locationType;

    @JsonProperty("startId")
    private String locationId;

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("modes")
    private Set<TransportMode> modes;

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("notes")
    private Boolean includeNotes;

    public DeparturesQueryDTO(LocalTime time, LocationType locationType, String locationId, boolean includeNotes) {
        this.time = time;
        this.locationType = locationType;
        this.locationId = locationId;
        this.includeNotes = includeNotes;
        modes = Collections.emptySet();
        // deserialisation
    }

    public DeparturesQueryDTO() {
        modes = Collections.emptySet();
        includeNotes = false;
        // deserialisation
    }

    public LocalTime getTime() {
        return time;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public String getLocationId() {
        return locationId;
    }

    public Set<TransportMode> getModes() {
        return modes;
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
                '}';
    }
}
