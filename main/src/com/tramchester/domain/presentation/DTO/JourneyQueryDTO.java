package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.time.TramTime;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

@JsonTypeName("JourneyQuery")
public class JourneyQueryDTO  {
    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("time")
    private LocalTime time;

    @JsonProperty("startType")
    private LocationType startType;

    @JsonProperty("startId")
    private String startId;

    @JsonProperty("destType")
    private LocationType destType;

    @JsonProperty("destId")
    private String destId;

    @JsonProperty("arriveBy")
    private boolean arriveBy;

    @JsonProperty("maxChanges")
    private int maxChanges;

    public JourneyQueryDTO() {
        // deserialisation
    }

    public JourneyQueryDTO(LocalDate date, LocalTime time, LocationType startType, String startId, LocationType destType, String destId,
                           boolean arriveBy, int maxChanges) {

        this.date = date;
        this.time = time;
        this.startType = startType;
        this.startId = startId;
        this.destType = destType;
        this.destId = destId;
        this.arriveBy = arriveBy;
        this.maxChanges = maxChanges;
    }

    public static JourneyQueryDTO create(LocalDate date, TramTime time, Location<?> start, Location<?> dest, boolean arriveBy, int maxChanges) {

        String startId = start.getId().forDTO();
        String destId = dest.getId().forDTO();
        LocationType startType = start.getLocationType();
        LocationType destType = dest.getLocationType();

        return new JourneyQueryDTO(date, time.asLocalTime(), startType, startId, destType, destId, arriveBy, maxChanges);

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
                '}';
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

    public String getStartId() {
        return startId;
    }

    public LocationType getDestType() {
        return destType;
    }

    public String getDestId() {
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
        return startId!=null && startType!=null && destId!=null && destType!=null && date!=null;
    }
}
