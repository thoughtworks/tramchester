package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Set;

@JsonTypeName("JourneyQuery")
public class JourneyQueryDTO  {
    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("time")
    private LocalTime time;

    @JsonProperty("startType")
    private LocationType startType;

    @JsonProperty("startId")
    private IdForDTO startId;

    @JsonProperty("destType")
    private LocationType destType;

    @JsonProperty("destId")
    private IdForDTO destId;

    @JsonProperty("arriveBy")
    private boolean arriveBy;

    @JsonProperty("maxChanges")
    private int maxChanges;

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("modes")
    private Set<TransportMode> modes;

    public JourneyQueryDTO() {
        modes = Collections.emptySet();
        // deserialisation
    }

    public JourneyQueryDTO(LocalDate date, LocalTime time, LocationType startType, IdForDTO startId, LocationType destType,
                           IdForDTO destId, boolean arriveBy, int maxChanges) {

        this.date = date;
        this.time = time;
        this.startType = startType;
        this.startId = startId;
        this.destType = destType;
        this.destId = destId;
        this.arriveBy = arriveBy;
        this.maxChanges = maxChanges;
        this.modes = Collections.emptySet();
    }

    public static JourneyQueryDTO create(LocalDate date, TramTime time, Location<?> start, Location<?> dest, boolean arriveBy, int maxChanges) {

        IdForDTO startId = IdForDTO.createFor(start);
        IdForDTO destId = IdForDTO.createFor(dest);
        LocationType startType = start.getLocationType();
        LocationType destType = dest.getLocationType();

        return new JourneyQueryDTO(date, time.asLocalTime(), startType, startId, destType, destId, arriveBy, maxChanges);

    }

    public static JourneyQueryDTO create(TramDate date, TramTime time, Location<?> start, Location<?> dest,
                                         boolean arriveBy, int maxChanges) {
        return create(date.toLocalDate(), time, start, dest, arriveBy, maxChanges);
    }

    @Override
    public String toString() {
        return "JourneyQueryDTO{" +
                "date=" + date +
                ", time=" + time +
                ", startType=" + startType +
                ", startId='" + startId + '\'' +
                ", destType=" + destType +
                ", destId='" + destId + '\'' +
                ", arriveBy=" + arriveBy +
                ", maxChanges=" + maxChanges +
                ", modes=" + modes +
                '}';
    }

    @JsonIgnore
    public TramDate getTramDate() {
        return TramDate.of(date);
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    public LocationType getStartType() {
        return startType;
    }

    public IdForDTO getStartId() {
        return startId;
    }

    public LocationType getDestType() {
        return destType;
    }

    public IdForDTO getDestId() {
        return destId;
    }

    public boolean isArriveBy() {
        return arriveBy;
    }

    public int getMaxChanges() {
        return maxChanges;
    }

    @JsonIgnore
    public boolean valid() {
        return startId!=null && startType!=null && destId!=null && destType!=null && date!=null
                && modes!=null;
    }

    public void setModes(Set<TransportMode> modes) {
        this.modes = modes;
    }

    public Set<TransportMode> getModes() {
        return modes;
    }
}
