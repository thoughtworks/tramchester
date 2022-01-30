package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tramchester.domain.places.LocationType;

import java.time.LocalDate;
import java.time.LocalTime;

@JsonTypeName("JourneryQuery")
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
}
