package com.tramchester.domain.liveUpdates;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.mappers.DateTimeJsonDeserializer;
import com.tramchester.mappers.DateTimeJsonSerializer;
import com.tramchester.mappers.LocalTimeJsonSerializer;
import org.joda.time.DateTime;

import java.util.LinkedList;
import java.util.List;

public class StationDepartureInfo {

    private String lineName;
    private String stationPlatform;
    private String message;
    private List<DueTram> dueTrams;
    private DateTime lastUpdate;
    private String displayId;

    public StationDepartureInfo(String displayId, String lineName, String stationPlatform, String message, DateTime lastUpdate) {
        this.displayId = displayId;
        this.lineName = lineName;
        this.stationPlatform = stationPlatform;
        this.message = message;
        this.lastUpdate = lastUpdate;
        dueTrams = new LinkedList<>();
    }

    public StationDepartureInfo() {
        // deserialisation
    }

    public String getLineName() {
        return lineName;
    }

    public String getStationPlatform() {
        return stationPlatform;
    }

    public String getMessage() {
        return message;
    }

    public List<DueTram> getDueTrams() {
        return dueTrams;
    }

    @JsonSerialize(using = DateTimeJsonSerializer.class)
    @JsonDeserialize(using = DateTimeJsonDeserializer.class)
    public DateTime getLastUpdate() {
        return lastUpdate;
    }

    @JsonIgnore
    public void addDueTram(DueTram dueTram) {
        dueTrams.add(dueTram);
    }

    public String getDisplayId() {
        return displayId;
    }

    @Override
    public String toString() {
        return "StationDepartureInfo{" +
                "lineName='" + lineName + '\'' +
                ", stationPlatform='" + stationPlatform + '\'' +
                ", message='" + message + '\'' +
                ", dueTrams=" + dueTrams +
                ", lastUpdate=" + lastUpdate +
                ", displayId=" + displayId +
                '}';
    }

}
